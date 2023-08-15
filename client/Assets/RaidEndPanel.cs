using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;

public class RaidEndPanel : MonoBehaviour
{
    public const float TRANSITION_IN_TIME = 2f;

    public static RaidEndPanel instance;

    public TMPro.TextMeshProUGUI titleText, attackerNameText, defenderNameText, attackerMedalsText, defenderMedalsText, percentageText, medalsRewardText, creditsRewardText, xpRewardText, rebirthRewardText;
    public GUITransition star1Trans, star2Trans, star3Trans, star4Trans, star5Trans, glow1Trans, glow2Trans, glow3Trans, glow4Trans, glow5Trans;

    float transitionStartTime, transitionEndTime;
    int numStars;

    public RaidEndPanel()
    {
        instance = this;
    }

	// Use this for initialization
	void OnEnable()
    {
        // Do nothing if the game is just initializing.
        if (GameData.instance == null) {
            return;
        }

        // Determine the number of stars.
        numStars = GameData.instance.DetermineNumRaidStars(GameData.instance.raidFlags);

        // Display the appropriate title text.
        switch (numStars)
        {
            case 0:
                titleText.text = LocalizationManager.GetTranslation("Raid/raid_end_defeat");
                Sound.instance.PlayMusic(Sound.instance.musicBuild, false, 0f, -1, 0f, 0f);
                break;
            case 1:
            case 2:
                titleText.text = LocalizationManager.GetTranslation("Raid/raid_end_partial_victory");
                Sound.instance.PlayMusic(Sound.instance.musicMinorAchievement, false, 1f, -1, 0f, 0f);
                break;
            case 3:
            case 4:
                titleText.text = LocalizationManager.GetTranslation("Raid/raid_end_victory");
                Sound.instance.PlayMusic(Sound.instance.musicMajorAchievement2, false, 2f, -1, 0f, 0f);
                break;
            case 5:
                titleText.text = LocalizationManager.GetTranslation("Raid/raid_end_total_victory");
                Sound.instance.PlayMusic(Sound.instance.musicMajorAchievement1, false, 1.9f, -1, 0f, 0f);
                break;
        }

        // Initialize all stars to invisible
        star1Trans.SetState(0f, 1f);
        star2Trans.SetState(0f, 1f);
        star3Trans.SetState(0f, 1f);
        star4Trans.SetState(0f, 1f);
        star5Trans.SetState(0f, 1f);
        glow1Trans.SetState(0f, 1f);
        glow2Trans.SetState(0f, 1f);
        glow3Trans.SetState(0f, 1f);
        glow4Trans.SetState(0f, 1f);
        glow5Trans.SetState(0f, 1f);

        StartCoroutine(ShowStars());
        StartCoroutine(TransitionIn());
	}
	
    public IEnumerator TransitionIn()
    {
        transitionStartTime = Time.unscaledTime;
        transitionEndTime = transitionStartTime + TRANSITION_IN_TIME;

        // Set the activity of the reward text fields
        creditsRewardText.gameObject.SetActive(GameData.instance.raidRewardCredits != 0);
        xpRewardText.gameObject.SetActive(GameData.instance.raidRewardXP != 0);
        rebirthRewardText.gameObject.SetActive(GameData.instance.raidRewardRebirth!= 0);

        //medalsRewardText.color = (GameData.instance.raidAttackerRewardMedals > 0) ? Color.green : ((GameData.instance.raidAttackerRewardMedals < 0) ? Color.red : Color.yellow);
        //creditsRewardText.color = (GameData.instance.raidRewardCredits > 0) ? Color.green : ((GameData.instance.raidRewardCredits < 0) ? Color.red : Color.yellow);
        //xpRewardText.color = (GameData.instance.raidRewardXP > 0) ? Color.green : ((GameData.instance.raidRewardXP < 0) ? Color.red : Color.yellow);

        while (Time.unscaledTime < transitionEndTime)
        {
            UpdateForTime();
            yield return null;
        }

        // Perform final update at end of transition in.
        UpdateForTime();
    }

    public void UpdateForTime()
    {
        int cur_val, delta;
        string cur_text;

        float progress = Math.Min(1f, (Time.unscaledTime - transitionStartTime) / TRANSITION_IN_TIME);

        // Attacker name text
        if (attackerNameText.text.Equals(GameData.instance.nationName) == false) {
            attackerNameText.text = GameData.instance.nationName;
        }

        // Defender name text
        if (defenderNameText.text.Equals(GameData.instance.raidDefenderNationName) == false) {
            defenderNameText.text = GameData.instance.raidDefenderNationName;
        }

        // Percentage
        cur_val = (int)(progress * GameData.instance.raidPercentageDefeated);
        cur_text = string.Format("{0:n0}", cur_val) + "%";
        if (cur_text.Equals(percentageText.text) == false) {
            percentageText.text = cur_text;
        }

        // Attacker medals
        delta = GameData.instance.raidAttackerRewardMedals;
        cur_val = GameData.instance.raidAttackerNationNumMedals + (int)(progress * delta);
        cur_text = string.Format("{0:n0}", cur_val) + " <sprite=30>";
        if (cur_text.Equals(attackerMedalsText.text) == false) {
            attackerMedalsText.text = cur_text;
        }

        // Defender medals
        delta = GameData.instance.raidDefenderRewardMedals;
        cur_val = GameData.instance.raidDefenderNationNumMedals + (int)(progress * delta);
        cur_text = string.Format("{0:n0}", cur_val) + " <sprite=30>";
        if (cur_text.Equals(defenderMedalsText.text) == false) {
            defenderMedalsText.text = cur_text;
        }

        // Medals reward
        delta = GameData.instance.raidAttackerRewardMedals;
        cur_val = (int)(progress * delta);
        cur_text = ((GameData.instance.raidAttackerRewardMedals >= 0) ? "<color=green>" : "<color=red>") + string.Format("{0:+#;-#;+0}", cur_val) + "</color> <sprite=30>";
        if (cur_text.Equals(medalsRewardText.text) == false) {
            medalsRewardText.text = cur_text;
        }

        // Credits reward
        delta = GameData.instance.raidRewardCredits;
        cur_val = (int)(progress * delta);
        cur_text = "<color=green>" + string.Format("{0:+#;-#;+0}", cur_val) + "</color> <sprite=2>";
        if (cur_text.Equals(creditsRewardText.text) == false) {
            creditsRewardText.text = cur_text;
        }

        // XP reward
        delta = GameData.instance.raidRewardXP;
        cur_val = (int)(progress * delta);
        cur_text = "<color=green>" + string.Format("{0:+#;-#;+0}", cur_val) + "</color> " + LocalizationManager.GetTranslation("Generic Text/xp_word");
        if (cur_text.Equals(xpRewardText.text) == false) {
            xpRewardText.text = cur_text;
        }

        // Rebirth reward
        delta = GameData.instance.raidRewardRebirth;
        cur_val = (int)(progress * delta);
        cur_text = "<color=green>" + string.Format("{0:+#;-#;+0}", cur_val) + "</color> " + LocalizationManager.GetTranslation("Generic Text/rebirth_word");
        if (cur_text.Equals(rebirthRewardText.text) == false) {
            rebirthRewardText.text = cur_text;
        }
    }

    public IEnumerator ShowStars()
    {
        // If there are no stars, do nothing else.
        if (numStars == 0) yield break;

        // Show the first star.
        StartCoroutine(ShowStar(star1Trans, glow1Trans));

        // If there are no more stars, do nothing else.
        if (numStars == 1) yield break;

        // Delay before next star
        yield return new WaitForSeconds(0.5f);

        // Show the second star.
        StartCoroutine(ShowStar(star2Trans, glow2Trans));

        // If there are no more stars, do nothing else.
        if (numStars == 2) yield break;

        // Delay before next star
        yield return new WaitForSeconds(0.5f);

        // Show the third star.
        StartCoroutine(ShowStar(star3Trans, glow3Trans));

        // If there are no more stars, do nothing else.
        if (numStars == 3) yield break;

        // Delay before next star
        yield return new WaitForSeconds(0.5f);

        // Show the fourth star.
        StartCoroutine(ShowStar(star4Trans, glow4Trans));

        // If there are no more stars, do nothing else.
        if (numStars == 4) yield break;

        // Delay before next star
        yield return new WaitForSeconds(0.5f);

        // Show the fifth star.
        StartCoroutine(ShowStar(star5Trans, glow5Trans));
    }

    public IEnumerator ShowStar(GUITransition _starTrans, GUITransition _glowTrans)
    {
        _glowTrans.StartTransition(0f, 0.5f, 0.5f, 1f, false);

        yield return new WaitForSeconds(0.4f);

        _starTrans.StartTransition(0f, 1f, 2f, 1f, false);

        yield return new WaitForSeconds(0.4f);

        Sound.instance.Play2D(Sound.instance.star_appear, 0.25f, true);

        _glowTrans.StartTransition(0.5f, 0.4f, 1f, 0.75f, false);
    }

    public void OnClickReturnHome()
    {
        // Close the raid end panel.
        GameGUI.instance.CloseAllPanels();

        // If we're viewing the raid map still, switch over to homeland map.
        if (GameData.instance.mapMode == GameData.MapMode.RAID) {
            Network.instance.SendCommand("action=switch_map");
        }
    }
}
