using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class RaidEntry : MonoBehaviour
{
    public TMPro.TextMeshProUGUI nationNameText, nationMedalsText, timeAgoText, rewardsText, percentageText, resultText;
    public Button replayButton;
    public GameObject replayNotAvailableText, star1, star2, star3, star4, star5;

    int raidID, enemyNationID;
    float startTime;

    static float prevReplayRequestTime = 0f;

    public void Init(RaidLogRecord _record, bool _attack)
    {
        raidID = _record.raidID;
        enemyNationID = _record.enemyNationID;
        startTime = _record.startTime;

        nationNameText.text = _record.enemyNationName + " <size=12><sprite=3></size>";
        nationMedalsText.text = string.Format("{0:n0}", _record.enemyNationMedals) + " <size=13><sprite=30></size>";

        if (_attack)
        {
            rewardsText.text = ((_record.rewardMedals > 0) ? ("<color=green>+" + string.Format("{0:n0}", _record.rewardMedals) + "</color> <sprite=30>  ") : ((_record.rewardMedals < 0) ? ("<color=red>-" + string.Format("{0:n0}", -_record.rewardMedals) + "</color> <sprite=30>  ") : ""))
                + ((_record.rewardCredits > 0) ? ("<color=green>+" + string.Format("{0:n0}", _record.rewardCredits) + "</color> <sprite=2>  ") : ((_record.rewardCredits < 0) ? ("<color=red>-" + string.Format("{0:n0}", -_record.rewardCredits) + "</color> <sprite=2>  ") : ""))
                + ((_record.rewardXP > 0) ? ("<color=green>+" + string.Format("{0:n0}", _record.rewardXP) + "</color> " + LocalizationManager.GetTranslation("Generic Text/xp_word")) : ((_record.rewardXP < 0) ? ("<color=red>-" + string.Format("{0:n0}", -_record.rewardXP) + "</color> " + LocalizationManager.GetTranslation("Generic Text/xp_word")) : ""))
                + ((_record.rewardRebirth > 0) ? ("  <color=green>+" + string.Format("{0:n0}", _record.rewardRebirth) + "</color> " + LocalizationManager.GetTranslation("Raid/raid_to_rebirth")) : ((_record.rewardRebirth < 0) ? ("  <color=red>-" + string.Format("{0:n0}", -_record.rewardRebirth) + "</color> " + LocalizationManager.GetTranslation("Raid/raid_to_rebirth")) : ""));
        }
        else
        {
            rewardsText.text = ((_record.rewardMedals > 0) ? ("<color=green>+" + string.Format("{0:n0}", _record.rewardMedals) + "</color> <sprite=30>  ") : ((_record.rewardMedals < 0) ? ("<color=red>-" + string.Format("{0:n0}", -_record.rewardMedals) + "</color> <sprite=30>  ") : ""));
        }

        percentageText.text = string.Format("{0:n0}", _record.percentageDefeated) + "%";

        int numStars = GameData.instance.DetermineNumRaidStars(_record.flags);

        star1.SetActive(numStars > 0);
        star2.SetActive(numStars > 1);
        star3.SetActive(numStars > 2);
        star4.SetActive(numStars > 3);
        star5.SetActive(numStars > 4);

        if (_attack)
        {
            switch (numStars)
            {
                case 0: 
                case 1:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_defeat");
                    break;
                case 2:
                case 3:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_partial_victory");
                    break;
                case 4:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_victory");
                    break;
                case 5:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_total_victory");
                    break;
            }
        }
        else
        {
            switch (numStars)
            {
                case 0: 
                case 1:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_defense_won");
                    break;
                case 2:
                case 3:
                case 4:
                case 5:
                    resultText.text = LocalizationManager.GetTranslation("Raid/raid_end_defense_lost");
                    break;
            }
        }

        bool replay_available = ((_record.flags & (int)GameData.RaidFlags.REPLAY_AVAILABLE) != 0);
        replayButton.gameObject.SetActive(replay_available);
        replayNotAvailableText.SetActive(!replay_available);

        UpdateTimeAgoText();
    }

    public void UpdateTimeAgoText()
    {
        // If the game hasn't yet ben initialized, do nothing.
        if (GameData.instance == null) {
            return;
        }

        timeAgoText.text = GameData.instance.GetDurationText((int)(Time.unscaledTime - startTime)) + " " + LocalizationManager.GetTranslation("Generic Text/ago_word");
    }

    public void OnClick_NationName()
    {
        // Send request_nation_info event to the server.
        Network.instance.SendCommand("action=request_nation_info|targetNationID=" + enemyNationID);
    }

    public void OnClick_Replay()
    {
        // Do not allow multiple requests too close to one another.
        if ((Time.unscaledTime - prevReplayRequestTime) > 5f)
        {
            // Send replay_raid event to the server.
            Network.instance.SendCommand("action=replay_raid|raidID=" + raidID);

            prevReplayRequestTime = Time.unscaledTime;
        }
    }
}
