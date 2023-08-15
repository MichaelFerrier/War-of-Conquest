using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;

public class ReplayControls : MonoBehaviour
{
    public GUITransition transition;
    public TMPro.TextMeshProUGUI playButtonText, speedButtonText;

    private Mode mode = Mode.UNDEF;
    private float speed = 2f, displaySpeed = -1f;

    public static ReplayControls instance;

    public enum Mode
    {
        UNDEF,
        PLAYING,
        PAUSED,
        ENDED
    }

    public ReplayControls() {
		instance = this;
	}

    public void Update()
    {
        UpdateDisplay();
    }

    public void OnSwitchedMap()
    {
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY)
        {
            // We've entered a replay map. Show the controls.
            UpdateDisplay();
            gameObject.SetActive(true);
            transition.StartTransition(0f, 1f, 1f, 1f, false);
        }
        else if (gameObject.activeSelf)
        {
            // The controls are showing but we're leaving the replay. Have the controls disappear.
            transition.StartTransition(1f, 0f, 1f, 1f, true);

            // Set the timeScale back to 1, as we're no longer in a replay.
            Time.timeScale = 1;
        }
    }

    public void OnReplayBegin()
    {
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY)
        {
            // Set the replay's speed.
            Time.timeScale = speed;

            // We're showing a replay. Show the controls.
            UpdateDisplay();
            gameObject.SetActive(true);
            transition.StartTransition(0f, 1f, 1f, 1f, false);
        }
    }

    public void UpdateDisplay()
    {
        // Determine the new play mode.
        Mode newMode = (GameData.instance.replayCurTime >= GameData.instance.replayEndTime) ? Mode.ENDED : ((Time.timeScale == 0f) ? Mode.PAUSED : Mode.PLAYING);

        // If the play mode has changed...
        if (newMode != mode)
        {
            // Record the new play mode.
            mode = newMode;

            // Update the play button for the new play mode.
            switch (mode)
            {
                case Mode.PAUSED: playButtonText.text = "<sprite=31>"; break;
                case Mode.PLAYING: playButtonText.text = "<sprite=32>"; break;
                case Mode.ENDED: playButtonText.text = "<sprite=33>"; break;
            }
        }

        // If the speed has changed...
        if (displaySpeed != speed)
        {
            // Record the new speed.
            displaySpeed = speed;

            // Update the speed button for the new speed.
            speedButtonText.text = string.Format("{0:n0}", speed) + " X";
        }
    }

    public void OnClick_PlayButton()
    {
        if (mode == Mode.PLAYING)
        {
            // Pause.
            Time.timeScale = 0f;
        }
        else if (mode == Mode.PAUSED)
        {
            // Play.
            Time.timeScale = speed;
        }
        else if (mode == Mode.ENDED)
        {
            // Init replay.
            MapView.instance.InitReplay();

            // Queue all patches to be updated.
            MapView.instance.QueueAllPatchesModified(true);

            // Restart the replay.
            GameData.instance.raidDefenderArea = GameData.instance.raidDefenderStartingArea;
            GameData.instance.replayCurTime = -3f;
            Time.timeScale = speed;
            GameData.instance.replayEventIndex = 0;
            GameData.instance.raidFlags = 0;

            // Update the RaidScoreHeader for the start of the replay.
            RaidScoreHeader.instance.OnReplayBegin();
        }
    }

    public void OnClick_SpeedButton()
    {
        // Double speed, limiting to range 1x to 32x.
        speed *= 2f;
        if (speed >= 64f) {
            speed = 1f;
        }

        // Set the timeScale to the new speed.
        Time.timeScale = speed;
   }
}
