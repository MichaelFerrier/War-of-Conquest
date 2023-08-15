using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;

public class RaidScoreHeader : MonoBehaviour
{
    public GUITransition transition;
    public GUITransition[] starTransitions = new GUITransition[5];
    public TMPro.TextMeshProUGUI titleText, percentageText, timerText;

    public static RaidScoreHeader instance;

    int defenderNationID = -1, percentage = -1, secondsRemaining = -1;
    int numRaidStars = -1;
    bool timeOutAlertedServer = false;
    bool[] starStates = new bool[5];
    
    public RaidScoreHeader() {
		instance = this;
	}

    public void Update()
    {
        UpdateTimer();
    }

    public void OnEnable()
    {
        timeOutAlertedServer = false;
        //StartCoroutine(RunTimer());
    }

    public void InfoEventReceived()
    {
        timeOutAlertedServer = false;
    }

    public void OnSwitchedMap()
    {
        if ((GameData.instance.mapMode == GameData.MapMode.RAID) && (GameData.instance.raidDefenderNationID > 0) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) != 0) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) == 0))
        {
            // We've entered a raid map, and the raid has begun and has not yet finished. Or, we're showing a replay. Show the header.
            UpdateDisplay(false);
            gameObject.SetActive(true);
            transition.StartTransition(0f, 1f, 1f, 1f, false);
        }
        else if (gameObject.activeSelf && (GameData.instance.mapMode != GameData.MapMode.RAID) && (GameData.instance.mapMode != GameData.MapMode.REPLAY))
        {
            // The header is showing but we're leaving the raid map. Have it disappear.
            transition.StartTransition(1f, 0f, 1f, 1f, true);
        }
    }

    public void OnReplayBegin()
    {
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY)
        {
            // Reset the nationID record, so that the whole header will be updated.
            defenderNationID = -1;

            // We're showing a replay. Show the header.
            UpdateDisplay(false);
            gameObject.SetActive(true);
            transition.StartTransition(0f, 1f, 1f, 1f, false);
        }
    }

    public void OnDefenderAreaUpdate(int _delay)
    {
        // If the header is active, update it. 
        if (gameObject.activeInHierarchy) {
            StartCoroutine(OnDefenderAreaUpdate_Coroutine(_delay));
        }
    }

    public IEnumerator OnDefenderAreaUpdate_Coroutine(int _delay)
    {
        yield return new WaitForSeconds(_delay);
        UpdateDisplay(true);
    }

    public void OnRaidStatusEvent(int _delay)
    {
        // If the header isn't active, and we're viewing a raid that has begun but has not finished, activate the header, but hide it.
        if ((gameObject.activeInHierarchy == false) && (GameData.instance.mapMode == GameData.MapMode.RAID) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) != 0) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) == 0)) 
        {
            gameObject.SetActive(true);
            transition.SetState(0f, 1f);
        }       

        // If the header is active, update it. 
        if (gameObject.activeInHierarchy) {
            StartCoroutine(OnRaidStatusEvent_Coroutine(_delay));
        }
    }

    public IEnumerator OnRaidStatusEvent_Coroutine(int _delay)
    {
        yield return new WaitForSeconds(_delay);

        if ((GameData.instance.mapMode != GameData.MapMode.RAID) || ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) != 0))
        {
            if (GameData.instance.mapMode == GameData.MapMode.RAID) {
                yield return new WaitForSeconds(2f); // The raid has just finished; wait a bit before having the score header disappear.
            }

            // Either we're not showing the raid map, or the raid has finished. Have the header disappear.
            transition.StartTransition(1f, 0f, 1f, 1f, true);
            yield break;
        }
        else if ((GameData.instance.mapMode == GameData.MapMode.RAID) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) != 0) && ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) == 0))
        {
            if (transition.GetAlpha() == 0f)
             {
                // We're viewing a raid that has begun and has not yet finished. The header is active but hidden. Transition it to being shown.
                UpdateDisplay(false);
                transition.StartTransition(0f, 1f, 1f, 1f, false);
             }
            else
            {
                // Just update the display, transitioning in any changes.
                UpdateDisplay(true);
            }
        }
    }

    public void UpdateDisplay(bool _transition)
    {
        if (defenderNationID != GameData.instance.raidDefenderNationID) 
        {
            // Compose title text
            if (GameData.instance.mapMode == GameData.MapMode.RAID) {
                titleText.text = LocalizationManager.GetTranslation("Generic Text/raid_word") + ": " + GameData.instance.raidDefenderNationName;
            } else {
                titleText.text = LocalizationManager.GetTranslation("Generic Text/replay_word") + ": " + GameData.instance.raidAttackerNationName + " " + LocalizationManager.GetTranslation("Raid/raid_end_vs") + " " + GameData.instance.raidDefenderNationName;
            }

            defenderNationID = GameData.instance.raidDefenderNationID;
        }

        // Determine the new current number of raid stars.
        int newNumRaidStars = GameData.instance.DetermineNumRaidStars(GameData.instance.raidFlags);

        if (numRaidStars != newNumRaidStars)
        {
            // Record the new current number of raid stars.
            numRaidStars = newNumRaidStars;

            // Set the state of each of the star graphics.
            SetStarState(0, numRaidStars > 0, _transition);
            SetStarState(1, numRaidStars > 1, _transition);
            SetStarState(2, numRaidStars > 2, _transition);
            SetStarState(3, numRaidStars > 3, _transition);
            SetStarState(4, numRaidStars > 4, _transition);
        }

        // Determine the new percentage.
        int newPercentage = Mathf.Max(0, 100 - (int)(GameData.instance.raidDefenderArea * 100 / GameData.instance.raidDefenderStartingArea));

        if (percentage != newPercentage)
        {
            // Record the new percentage.
            percentage = newPercentage;

            // Display the new percentage.
            percentageText.text = string.Format("{0:n0}", percentage) + "%";
        }

        // Update the timer.
        UpdateTimer();
    }
    /*
    public IEnumerator RunTimer()
    {
        for (;;)
        {
            UpdateTimer();
            yield return new WaitForSeconds(1);
        }
    }
    */
    public void UpdateTimer()
    {
        // Do nothing if still initializing the game.
        if (GameData.instance == null) {
            return;
        }

        // Determine the new number of seconds remaining.
        int newSecondsRemaining = (int)Mathf.Max(0, (GameData.instance.mapMode == GameData.MapMode.RAID) ? (GameData.instance.raidEndTime - Time.unscaledTime) : (GameData.instance.replayEndTime - GameData.instance.replayCurTime));

        if (secondsRemaining != newSecondsRemaining)
        {
            // Record the new number of seconds remaining.
            secondsRemaining = newSecondsRemaining;

            // Update the timer text.
            timerText.text = GameData.instance.GetDurationClockText(secondsRemaining);
        }

        if (GameData.instance.mapMode == GameData.MapMode.RAID)
        {
            // If this raid has timed out, notify the server.
            if ((secondsRemaining == 0) && !timeOutAlertedServer) 
            {
                Network.instance.SendCommand("action=raid_timeout");
                timeOutAlertedServer = true;
            }
        }
    }

    public void SetStarState(int _index, bool _state, bool _transition)
    {
        if (_transition)
        {
            if (starStates[_index] != _state)
            {
                // Transition to the star's new state.
                if (_state) {
                    starTransitions[_index].StartTransition(0f, 1f, 3f, 1f, false);
                } else {
                    starTransitions[_index].StartTransition(1f, 0f, 1f, 3f, false);
                }
            }
        }
        else
        {
            // Imediately set the star's state.
            starTransitions[_index].SetState(_state ? 1f : 0f, 1f);
        }

        starStates[_index] = _state;
    }
}
