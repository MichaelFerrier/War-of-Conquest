using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;

public class RaidButton : MonoBehaviour, RequestorListener
{
    public enum Mode
    {
        UNDEF,
        AWAITING_MANPOWER,
        START_RAID,
        JOIN_RAID,
        NEXT_RAID,
        ABORT_RAID,
        END_RAID,
        GO_HOME,
        END_REPLAY
    };

    public TMPro.TextMeshProUGUI text;
    public GUITransition transition;
    public Mode mode = Mode.UNDEF;
    public float awaitingManpowerDoneTime;

    public static RaidButton instance;

    static float prevSendRequestTime = 0f;

    void Awake() {
		instance = this;
	}

    public void OnEnable()
    {
        if (mode == Mode.AWAITING_MANPOWER) {
            StartCoroutine(RunManpowerTime());
        }
    }

    public void OnSwitchedMap()
    {
        // Show the button only if appropriate.
        UpdateObjectVisibility();
    }

    public void OnUpdateEvent()
    {
        if (gameObject.activeInHierarchy) {
            StartCoroutine(UpdateMode(true));
        }
    }

    public void OnStatsEvent()
    {
        if (gameObject.activeInHierarchy) {
            StartCoroutine(UpdateMode(true));
        }
    }

    public void OnBuildShard()
    {
        if (gameObject.activeInHierarchy) {
            StartCoroutine(UpdateMode(true));
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
        OnRaidStatusEvent();
    }

    public void OnRaidStatusEvent()
    {
        if (gameObject.activeSelf) {
            StartCoroutine(UpdateMode(true)); // Update the mode, fading out before changing it.
        }
    }

    public void OnClick()
    {
        // Do not respond to multiple clicks in rapid succession.
        if ((Time.unscaledTime - prevSendRequestTime) < 2f) {
            return;
        }

        if ((mode == Mode.ABORT_RAID) || (mode == Mode.GO_HOME))
        {
            // Switch back to homeland map.
            Network.instance.SendCommand("action=switch_map");
        }
        else if (mode == Mode.END_REPLAY)
        {
            // Switch back to homeland map.
            Network.instance.SendCommand("action=end_replay");
        }
        else if (mode == Mode.END_RAID)
        {
            // "End this raid now?"
            Requestor.Activate((int)mode, 0, this, LocalizationManager.GetTranslation("Raid/raid_request_end_raid"), LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
        }
        else if (mode == Mode.AWAITING_MANPOWER)
        {
            // "We'll be able to launch a raid once our home island's manpower is {min_percent}% full. We can wait for manpower to build up, or buy it."
            Requestor.Activate((int)mode, 0, this, LocalizationManager.GetTranslation("Raid/raid_request_buy_manpower").Replace("{min_percent}", string.Format("{0:n0}", (int)(GameData.instance.minManpowerFractionToStartRaid * 100f + 0.5f))), LocalizationManager.GetTranslation("Generic Text/wait_word"), LocalizationManager.GetTranslation("Generic Text/buy_word"));
        }
        else
        {
            // Send raid event to server.
            Network.instance.SendCommand("action=raid");
        }

        prevSendRequestTime = Time.unscaledTime;
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        //Debug.Log("Here. mapMode: " + GameData.instance.mapMode + ", mode: " + mode + ", _data: " + _data + ", result: " + _result);
        // If we're still on the raid map, and the raid mode hasn't changed since the requestor appeared...
        if ((int)mode == _task)
        {
            if ((mode == Mode.END_RAID) && (GameData.instance.mapMode == GameData.MapMode.RAID))
            {
                if (_result == Requestor.RequestorButton.LeftButton) 
                {
                    // Send raid command to end the current raid.
                    Network.instance.SendCommand("action=raid");
                }
            }
            else if ((mode == Mode.AWAITING_MANPOWER) && (GameData.instance.mapMode == GameData.MapMode.HOMELAND))
            {
                if (_result == Requestor.RequestorButton.RightButton) 
                {
                    // Open the dialog to buy manpower.
                    GameGUI.instance.OpenBuyDialog(BuyPanel.BuyType.Manpower);
                }
            }
        }
    }

    public void UpdateObjectVisibility()
    {
        //Debug.Log("RaidButton UpdateObjectVisibility() mapMode: " + GameData.instance.mapMode);

        if ((GameData.instance.mapMode == GameData.MapMode.HOMELAND) || (GameData.instance.mapMode == GameData.MapMode.RAID) || (GameData.instance.mapMode == GameData.MapMode.REPLAY))
        {
            this.gameObject.SetActive(true);

            // Update the button's mode for the current raid status.
            StartCoroutine(UpdateMode(false));
        }
        else
        {
            // Hide the button.
            this.gameObject.SetActive(false);
            mode = Mode.UNDEF;
        }
    }

    public IEnumerator UpdateMode(bool _fade_out)
    {
        Mode new_mode = Mode.UNDEF;

        // Determine the new mode.
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY)
        {
            new_mode = Mode.END_REPLAY;
        }
        else if (GameData.instance.raidDefenderNationID == -1)
        {
            // No raid is currently active for this nation.

            if (GameData.instance.DetermineNumShardsPlaced() < 3) {
                new_mode = Mode.UNDEF;
            } else if (GameData.instance.homeland_footprint.manpower < (GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid)) {
                new_mode = Mode.AWAITING_MANPOWER;
            } else {
                new_mode = Mode.START_RAID;
            }
        }
        else
        {
            // A raid is currently active for this nation.

            if (GameData.instance.mapMode == GameData.MapMode.RAID)
            {
                // This player is viewing the active raid.

                if ((GameData.instance.raidFlags & (int)GameData.RaidFlags.BEGUN) == 0)
                {
                    // The active raid hasn't yet begun.

                    if (GameData.instance.homeland_footprint.manpower < (GameData.instance.GetHomelandManpowerMax() * (GameData.instance.minManpowerFractionToStartRaid + GameData.instance.manpowerFractionCostToRestartRaid))) {
                        // There isn't enough manpower to start a new raid. Only give the option to end this raid.
                        new_mode = Mode.ABORT_RAID;
                    } else {
                        // There's enough manpower to start a new raid.
                        new_mode = Mode.NEXT_RAID;
                    }
                }
                else if ((GameData.instance.raidFlags & (int)GameData.RaidFlags.FINISHED) == 0)
                {
                    // Allow the player to end this raid.
                    new_mode = Mode.END_RAID;
                }
                else
                {
                    // The raid is already finished. Allow the player to return to their home island.
                    new_mode = Mode.GO_HOME;
                }
            }
            else
            {
                // This player is not viewing the active raid; allow them to join it.
                new_mode = Mode.JOIN_RAID;
            }
        }

        //Debug.Log("UpdateMode(): raidFlags: " + GameData.instance.raidFlags + ", defenderNationID: " + GameData.instance.raidDefenderNationID + ", mp: " + GameData.instance.homeland_footprint.manpower + ", start mp: " + (GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid) + ", restart mp: " + (GameData.instance.GetHomelandManpowerMax() * (GameData.instance.minManpowerFractionToStartRaid + GameData.instance.manpowerFractionCostToRestartRaid)) + ", new_mode: " + new_mode + ", old mode: " + mode);

        // If awaiting manpower, determine time until enough manpower will have been generated.
        if (new_mode == Mode.AWAITING_MANPOWER)
        {
            Debug.Log("missing mp: " + ((GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid) - GameData.instance.homeland_footprint.manpower) + ", mp rate: " + GameData.instance.GetHomelandManpowerRate() + ", time to accrue: " + (((GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid) - GameData.instance.homeland_footprint.manpower) * 3600f / GameData.instance.GetHomelandManpowerRate()));
            awaitingManpowerDoneTime = Time.unscaledTime + 30f + (((GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid) - GameData.instance.homeland_footprint.manpower) * 3600f / GameData.instance.GetHomelandManpowerRate());
        }

        // If the new mode is the same as the old mode, do nothing.
        if (new_mode == mode) {
            yield break;
        }

        // Change the mode.
        Mode old_mode = mode;
        mode = new_mode;

        // If _fade_out is true, fade out the button before changing appearance for the new mode.
        if (_fade_out && (old_mode != Mode.UNDEF)) 
        {
            transition.StartTransition(1f, 0f, 1f, 1f, false);
            yield return new WaitForSeconds(transition.transitionDuraton);
        }

        switch (mode)
        {
            case Mode.AWAITING_MANPOWER:
                StartCoroutine(RunManpowerTime());
                break;
            case Mode.START_RAID:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_raid");
                break;
            case Mode.NEXT_RAID:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_next") + "\n" + (int)(GameData.instance.GetHomelandManpowerMax() * GameData.instance.manpowerFractionCostToRestartRaid + 0.5f) + " <size=15><sprite=0></size>";
                break;
            case Mode.JOIN_RAID:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_join_raid");
                break;
            case Mode.ABORT_RAID:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_home");
                break;
            case Mode.END_RAID:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_end_raid");
                break;
            case Mode.GO_HOME:
            case Mode.END_REPLAY:
                text.text = LocalizationManager.GetTranslation("Raid/raid_button_home");
                break;
        }

        // Fade the button back in.
        transition.StartTransition(0f, 1f, 1f, 1f, false);
    }

    public IEnumerator RunManpowerTime()
    {
        while (mode == Mode.AWAITING_MANPOWER)
        {
            // Update timer text
            text.text = DetermineAwaitingManpowerText();

            // Pause for a second
            yield return new WaitForSeconds(1);

            // If homeland manpower is now enough to start a raid, update mode and exit loop.
            if (GameData.instance.homeland_footprint.manpower >= (GameData.instance.GetHomelandManpowerMax() * GameData.instance.minManpowerFractionToStartRaid))
            {
                UpdateMode(true);
                break;
            }
        }
    }

    public string DetermineAwaitingManpowerText()
    {
        return GameData.instance.GetDurationClockText((int)(awaitingManpowerDoneTime - Time.unscaledTime)) + " <size=15><sprite=0></size>\n" + LocalizationManager.GetTranslation("Raid/raid_button_to_raid");
    }
}
