using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class SwitchMapButton : MonoBehaviour
{
    public enum Mode
    {
        UNDEF,
        TO_HOME_ISLAND,
        TO_MAIN_WORLD
    };

    public TMPro.TextMeshProUGUI topText, bottomText;
    public Image image;
    public Sprite spriteHomeIsland, spriteMainLand;
    public GUITransition transition;
    public Mode mode = Mode.UNDEF;

    public static SwitchMapButton instance;

    static float prevSendRequestTime = 0f;

    void Awake() {
		instance = this;
	}

    public void OnSwitchedMap()
    {
        // Show the button only if appropriate.
        UpdateObjectVisibility();
    }

    public void OnClick()
    {
        // Do not respond to multiple clicks in rapid succession.
        if ((Time.unscaledTime - prevSendRequestTime) < 2f) {
            return;
        }

        // Switch between homeland and mainland map.
        Network.instance.SendCommand("action=switch_map");
        
        prevSendRequestTime = Time.unscaledTime;
    }

    public void UpdateObjectVisibility()
    {
        //Debug.Log("SwitchMapButton UpdateObjectVisibility() mapMode: " + GameData.instance.mapMode);

        if ((GameData.instance.mapMode == GameData.MapMode.HOMELAND) || (GameData.instance.mapMode == GameData.MapMode.MAINLAND) || (GameData.instance.mapMode == GameData.MapMode.RAID))
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
        if (GameData.instance.mapMode == GameData.MapMode.HOMELAND)
        {
            new_mode = Mode.TO_MAIN_WORLD;
        }
        else if ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) || (GameData.instance.mapMode == GameData.MapMode.RAID))
        {
            new_mode = Mode.TO_HOME_ISLAND;
        }
        else
        {
            new_mode = Mode.UNDEF;
        }

        // If the new mode is the same as the old mode, do nothing.
        if (new_mode == mode) {
            yield break;
        }

        // Change the mode.
        Mode old_mode = mode;
        mode = new_mode;

        // If _fade_out is true, fade out the button before changing appearance forthe new mode.
        if (_fade_out && (old_mode != Mode.UNDEF)) 
        {
            transition.StartTransition(1f, 0f, 1f, 1f, false);
            yield return new WaitForSeconds(transition.transitionDuraton);
        }

        switch (mode)
        {
            case Mode.TO_HOME_ISLAND:
                topText.text = LocalizationManager.GetTranslation("Generic Text/button_home_island_1");
                bottomText.text = LocalizationManager.GetTranslation("Generic Text/button_home_island_2");
                image.sprite = spriteHomeIsland;
                break;
            case Mode.TO_MAIN_WORLD:
                topText.text = LocalizationManager.GetTranslation("Generic Text/button_main_world_1");
                bottomText.text = LocalizationManager.GetTranslation("Generic Text/button_main_world_2");
                image.sprite = spriteMainLand;
                break;
        }

        // Fade the button back in.
        transition.StartTransition(0f, 1f, 1f, 1f, false);
    }
}
