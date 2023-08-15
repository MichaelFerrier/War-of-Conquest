using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;

public class RaidIntroHeader : MonoBehaviour
{
    public TMPro.TextMeshProUGUI nationNameText, rewardsText;
    public GUITransition transition;
    private bool reviewEnded = false;

    public static RaidIntroHeader instance;

    public RaidIntroHeader() {
		instance = this;
	}

    public void OnEnable()
    {
        reviewEnded = false;
    }

    public void Update()
    {
        if ((reviewEnded == false) && (Time.unscaledTime >= GameData.instance.raidReviewEndTime))
        {
            // Send command to switch back to homeland map.
            Network.instance.SendCommand("action=switch_map");

            // Record that the review period for this raid has ended.
            reviewEnded = true;
        }
    }

    public void OnSwitchedMap()
    {
        if ((GameData.instance.mapMode == GameData.MapMode.RAID) && (GameData.instance.raidDefenderNationID > 0) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) == 0))
        {
            // We've entered a raid map, and the raid hasn't begun yet. Show the header.
            UpdateText();
            gameObject.SetActive(true);
            transition.StartTransition(0f, 1f, 1f, 1f, false);
        }
        else if (gameObject.activeSelf && (GameData.instance.mapMode != GameData.MapMode.RAID))
        {
            // The header is showing but we're leaving the raid map. Have it disappear.
            transition.StartTransition(1f, 0f, 1f, 1f, true);
        }
    }

    public void OnRaidStatusEvent(int _delay)
    {
        if (gameObject.activeInHierarchy) {
            StartCoroutine(OnRaidStatusEvent_Coroutine(_delay));
        }
    }

    public IEnumerator OnRaidStatusEvent_Coroutine(int _delay)
    {
        yield return new WaitForSeconds(_delay);

        if ((GameData.instance.mapMode != GameData.MapMode.RAID) || ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) != 0))
        {
            // Either we're not showing the raid map, or the raid has begun. Have the header disappear.
            transition.StartTransition(1f, 0f, 1f, 1f, true);
            yield break;
        }

        if (transition.GetAlpha() == 1f)
        {
            // Fade out the previous raid candidate's header before showing the header for the new raid candidate.
            transition.StartTransition(1f, 0f, 1f, 1f, false);
            yield return new WaitForSeconds(transition.transitionDuraton);
        }

        // Update the text for the new raid candidate.
        UpdateText();

        // Transition in the header.
        transition.StartTransition(0f, 1f, 1f, 1f, false);
    }

    public void UpdateText()
    {
        nationNameText.text = GameData.instance.raidDefenderNationName + " <color=white>" + string.Format("{0:n0}", GameData.instance.raidDefenderNationNumMedals) + "</color><size=20><sprite=30></size>";
        rewardsText.text = LocalizationManager.GetTranslation("Generic Text/win_word") + ": <color=green>+" + string.Format("{0:n0}", GameData.instance.raid5StarMedalDelta) + "</color><size=20><sprite=30></size>  <color=green>+" + string.Format("{0:n0}", GameData.instance.raidMaxRewardCredits) + "</color><size=20><sprite=2></size>  <color=green>+" + string.Format("{0:n0}", GameData.instance.raidMaxRewardXP) + "</color> XP" + ((GameData.instance.raidMaxRewardRebirth > 0) ? ("  <color=green>+" + string.Format("{0:n0}", GameData.instance.raidMaxRewardRebirth) + "</color> " + LocalizationManager.GetTranslation("Raid/raid_to_rebirth")) : "") + "\n" + LocalizationManager.GetTranslation("Generic Text/loss_word") + ": <color=#FF7744>" + ((GameData.instance.raid0StarMedalDelta == 0) ? "-" : "") + string.Format("{0:n0}", GameData.instance.raid0StarMedalDelta) + "</color><size=20><sprite=30></size>";
    }
}
