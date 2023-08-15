using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using System.Collections.Generic; // NEW
using I2.Loc;

public class DisplayAttack : MonoBehaviour
{
    public static ArrayList display_attack_list = new ArrayList();
    public static int num_in_cur_series = 0;
    public static float top_anchor_y, bottom_anchor_y;

    public CanvasGroup canvasGroup;
    public Image attack_stat_image, defend_stat_image, total_defense_image, insurgency_image;
    public Sprite attackSpriteTech, attackSpriteBio, attackSpritePsi;
    public TMPro.TextMeshProUGUI attack_nation_name, defend_nation_name;
    public Slider attack_points_slider, defend_points_slider;
    public Image attack_points_fill, defend_points_fill;

    //public Text attackerValText, defenderValText;
    //public GameObject attackerValTextObject, defenderValTextObject;
    public GameObject downArrowOnObject, downArrowOffObject, upArrowOnObject, upArrowOffObject;
    public RectTransform downIndicator, upIndicator; 

    public enum Position
    {
        UNDEF,
        ABOVE,
        BELOW
    };

    // The scale of this GUI object at the canvas scaler's target resolution. It will be automatically scaled from there according to the actual resolution.
    private const float TARGET_RESOLUTION_SCALE = 0.35f;

    private const float FADE_DURATION = 0.5f;

    private const float TOP_Y_MARGIN = 170;
    private const float BOTTOM_Y_MARGIN = 138;

    private RandomProgress battle_attacker_progress = new RandomProgress();
    private RandomProgress battle_defender_progress = new RandomProgress();

    private int x, z;
    private float start_time, end_time, finished_time, duration;
    private float attack_points_start, attack_points_end, attack_points_dif, attack_points_max;
    private float defend_points_start, defend_points_end, defend_points_dif, defend_points_max;
    private Position default_position, position = Position.UNDEF;
    private bool ended = false;

    private Color full_color = Color.green;
    private Color half_color = Color.yellow;
    private Color empty_color = Color.red;
    private Color slider_background_color = new Color(0.6f, 0.6f, 0.6f);

    public static GameObject ActivateNew(int _duration, int _x, int _z, float _attack_points_max, float _attack_points_start, float _attack_points_end, float _defend_points_max, float _defend_points_start, float _defend_points_end, int _attacker_stat, int _defender_stat, int _attacker_nation_ID, int _defender_nation_ID, int _battle_flags)
    {
        GameObject display_object = MemManager.instance.GetDisplayAttackObject();
        display_attack_list.Add(display_object);

        // Init and activate the DisplayAttack.
        DisplayAttack display = display_object.GetComponent<DisplayAttack>();
        display.Activate(_duration, _x, _z, _attack_points_max, _attack_points_start, _attack_points_end, _defend_points_max, _defend_points_start, _defend_points_end, _attacker_stat, _defender_stat, _attacker_nation_ID, _defender_nation_ID, _battle_flags);

        // Lock the block until the process is done, to prevent further clicks on it from this player.
        MapView.instance.LockBlock(_x, _z, Time.unscaledTime + _duration);

        return display_object;
    }

    private static void Deactivate(GameObject _display_object)
    {
        DisplayAttack display = _display_object.GetComponent<DisplayAttack>();

        // Deactivate this DisplayAttack
        display.Deactivate();

        // Free this DisplayAttack
        display_attack_list.Remove(_display_object);
        MemManager.instance.ReleaseDisplayAttackObject(_display_object);
    }

    public static void UpdateAllActivity()
    {
        // Update DisplayAttack objects.
        GameObject display_object;
        DisplayAttack display;

        // Iterate through all active DisplayAttack objects...
        for (int i = 0; i < display_attack_list.Count; i++)
        {
            // Get the current active DisplayAttack
            display_object = (GameObject)(display_attack_list[i]);
            display = display_object.GetComponent<DisplayAttack>();

            // Update the current active DisplayAttack and determine whether it's finished.
            if (display.UpdateActivity() == true)
            {
                // This DisplayAttack is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllForMapAreaChange()
    {
        GameObject display_object;
        DisplayAttack display;

        // Iterate through all active DisplayAttack objects...
        for (int i = 0; i < display_attack_list.Count; i++)
        {
            // Get the current active DisplayAttack
            display_object = (GameObject)(display_attack_list[i]);
            display = display_object.GetComponent<DisplayAttack>();

            // Update the current active DisplayAttack and determine whether it's finished.
            if (display.UpdateForViewChange() == true)
            {
                // This DisplayAttack is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllScreenPosition()
    {
        DisplayAttack display;

        DetermineAnchorPositions();

        // Update the screen positions of all DisplayAttack objects in use.
        for (int i = 0; i < display_attack_list.Count; i++)
        {
            GameObject display_object = (GameObject)(display_attack_list[i]);
            display = display_object.GetComponent<DisplayAttack>();
            display.UpdateScreenPosition();
        }
    }

    public static void DetermineAnchorPositions()
    {
        top_anchor_y = Screen.height - (TOP_Y_MARGIN  * MapView.instance.canvas.scaleFactor);
        bottom_anchor_y = (Chat.instance.GetChatHeight() + GameGUI.instance.GetMainUIBottomHeight() + BOTTOM_Y_MARGIN) * MapView.instance.canvas.scaleFactor;
    }

    public static void CancelForBlock(int _x, int _z)
    {
        GameObject display_object;
        DisplayAttack display;

        // Iterate through all active DisplayAttack objects...
        for (int i = 0; i < display_attack_list.Count; i++)
        {
            // Get the current active DisplayAttack
            display_object = (GameObject)(display_attack_list[i]);
            display = display_object.GetComponent<DisplayAttack>();

            // If the current active DisplayAttack is for the given block, cancel it.
            if ((display.x == _x) && (display.z == _z))
            {
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static int GetNumActive()
    {
        // Determine count of active DisplayAttack objects that have not ended yet (don't count those that are just fading away).
        int count = 0;
        for (int i = 0; i < display_attack_list.Count; i++)
        {
            // Get the current active DisplayProcess
            GameObject display_object = (GameObject)(display_attack_list[i]);
            DisplayAttack display = display_object.GetComponent<DisplayAttack>();

            if (display.end_time > Time.time) {
                count++;
            }
        }

        return count;
    }

    void Start()
    {
    }
       
    public void Activate(int _duration, int _x, int _z, float _attack_points_max, float _attack_points_start, float _attack_points_end, float _defend_points_max, float _defend_points_start, float _defend_points_end, int _attacker_stat, int _defender_stat, int _attacker_nation_ID, int _defender_nation_ID, int _battle_flags)
    {
        x = _x;
        z = _z;
        duration = (float)_duration;
        start_time = Time.time;
        end_time = start_time + duration;
        finished_time = end_time + FADE_DURATION;
        ended = false;

        attack_points_start = (float)_attack_points_start;
        attack_points_end = (float)_attack_points_end;
        attack_points_dif = attack_points_end - attack_points_start;
        attack_points_max = (float)_attack_points_max;

        defend_points_start = (float)_defend_points_start;
        defend_points_end = (float)_defend_points_end;
        defend_points_dif = defend_points_end - defend_points_start;
        defend_points_max = (float)_defend_points_max;

        NationData attack_nation_data = GameData.instance.GetNationData(_attacker_nation_ID);

        if (attack_nation_data != null)
        {
            // Set the attacking nation's name
            attack_nation_name.text = attack_nation_data.GetName(false);

            // Set the attacking nation's stat image
            switch (_attacker_stat)
            {
                case GameData.STAT_BIO: attack_stat_image.sprite = attackSpriteBio; break;
                case GameData.STAT_PSI: attack_stat_image.sprite = attackSpritePsi; break;
                default: attack_stat_image.sprite = attackSpriteTech; break;
            }

            //// Set the color of the attacker stat to the color of the attacker nation.
            //attack_stat_image.color = new Color(attack_nation_data.r, attack_nation_data.g, attack_nation_data.b);
            //if (attack_stat_image.color.grayscale < 0.2f) attack_stat_image.color = attack_stat_image.color + new Color(0.15f, 0.15f, 0.15f);
        }

        NationData defend_nation_data = GameData.instance.GetNationData(_defender_nation_ID);

        if (defend_nation_data != null)
        {
            // Set the defending nation's name
            defend_nation_name.text = defend_nation_data.GetName(true);

            // Set the defending nation's stat image
            switch (_defender_stat)
            {
                case GameData.STAT_BIO: defend_stat_image.sprite = attackSpriteBio; break;
                case GameData.STAT_PSI: defend_stat_image.sprite = attackSpritePsi; break;
                default: defend_stat_image.sprite = attackSpriteTech; break;
            }

            //// Set the color of the defender stat to the color of the attacker nation.
            //defend_stat_image.color = new Color(defend_nation_data.r, defend_nation_data.g, defend_nation_data.b);
            //if (defend_stat_image.color.grayscale < 0.2f) defend_stat_image.color = defend_stat_image.color + new Color(0.15f, 0.15f, 0.15f);
        }

        // Display the total defense and insurgency images only if the corresponding battle flag is set.
        total_defense_image.gameObject.SetActive((_battle_flags & (int)GameData.BattleFlags.TOTAL_DEFENSE) != 0);
        insurgency_image.gameObject.SetActive((_battle_flags & (int)GameData.BattleFlags.INSURGENCY) != 0);

        // Initialize battle progress information, so that it proceeds with randomness and acceleration.
        battle_attacker_progress.Init();
        battle_defender_progress.Init();
        
        // Parent this DisplayAttack to the overlay panel.
        transform.SetParent(MapView.instance.overlay_panel_2_rect_transform);

        // Have this latest display show below any older displays in this same overlay, since the other displays will disappear sooner.
        transform.SetAsFirstSibling();

        // Set initial scale for the canvas scaler's target resolution. Needs to be done each time it's activated, in case it was changed last time used.
        transform.localScale = new Vector3(TARGET_RESOLUTION_SCALE, TARGET_RESOLUTION_SCALE, 1.0f);

        // Initial alpha value.
        canvasGroup.alpha = 1f;

        //// Initialize value text displays
        //// NOTE -- disabled; may eventually depend on a setting.
        //attackerValTextObject.SetActive(false);
        //defenderValTextObject.SetActive(false);

        // Set the initial hit points slider values.
        SetHitPointsValues(attack_points_start, defend_points_start);

        // If the INERT battle flag is set, display text showing that the build object in the attacked block is inert.
        if ((_battle_flags & (int)GameData.BattleFlags.INERT) != 0)
        {
            // GB-Localization
            // "INERT"
            GameObject anim_text_object = MemManager.instance.GetAnimTextObject();
            AnimText anim_text = anim_text_object.GetComponent<AnimText>();
            anim_text.Activate(MapView.instance.canvas.transform, new Vector2(0,0), new Vector2(0,0), MapView.instance.GetBlockCenterScreenPos(_x, _z) / MapView.instance.canvas.scaleFactor, LocalizationManager.GetTranslation("attack_inert"));
        }

        // If the FLANKED battle flag is set, display text showing that the build object in the attacked block is flanked.
        else if ((_battle_flags & (int)GameData.BattleFlags.FLANKED) != 0)
        {
            // GB-Localization
            // "FLANKED"
            GameObject anim_text_object = MemManager.instance.GetAnimTextObject();
            AnimText anim_text = anim_text_object.GetComponent<AnimText>();
            anim_text.Activate(MapView.instance.canvas.transform, new Vector2(0,0), new Vector2(0,0), MapView.instance.GetBlockCenterScreenPos(_x, _z) / MapView.instance.canvas.scaleFactor, LocalizationManager.GetTranslation("attack_flanked"));
        }

        // If the CRIT battle flag is set, display text showing that this was a critical hit.
        else if ((_battle_flags & (int)GameData.BattleFlags.CRIT) != 0)
        {
            // GB-Localization
            // "CRITICAL"
            GameObject anim_text_object = MemManager.instance.GetAnimTextObject();
            AnimText anim_text = anim_text_object.GetComponent<AnimText>();
            anim_text.Activate(MapView.instance.canvas.transform, new Vector2(0,0), new Vector2(0,0), MapView.instance.GetBlockCenterScreenPos(_x, _z) / MapView.instance.canvas.scaleFactor, LocalizationManager.GetTranslation("attack_crit"));
        }

        // Reset position
        position = Position.UNDEF;

        // Determine this DisplayAttack's default position -- the first is positioned below, and every other one activated simultaneously in sequece alternates position.
        if (GetNumActive() == 1) {
            num_in_cur_series = 1;
        } else {
            num_in_cur_series++;
        }
        default_position = ((num_in_cur_series % 2) == 0) ? Position.ABOVE : Position.BELOW;

        DetermineAnchorPositions();

        // Perform initial update of screen position.
        UpdateScreenPosition();
    }

    private void Deactivate()
    {
        transform.SetParent(null);
    }

    public bool UpdateActivity()
    {
        // Get current time.
        float time = Time.time;

        if (time >= finished_time)
        {
            // All finished.
            return true;
        }
        else if (time >= end_time)
        {
            if (!ended)
            {
                // Record that this display has ended (it is now just fading out).
                ended = true;

                // Make sure the hit points sliders are displaying the final value.
                SetHitPointsValues(attack_points_end, defend_points_end);

                // Update for a process having ended.
                MapView.instance.ProcessEnded();
            }

            // The process has ended, now fading out the DisplayAttack.
            canvasGroup.alpha = 1.0f - ((time - end_time) / FADE_DURATION);
        }
        else
        {
            // Set the new hit points slider values.
            float progress = (time - start_time) / duration;
            float attacker_progress = battle_attacker_progress.GetRandomProgress(progress);
            float defender_progress = battle_defender_progress.GetRandomProgress(progress);
            SetHitPointsValues((attacker_progress * attack_points_dif + attack_points_start), (defender_progress * defend_points_dif + defend_points_start));
        }

        return false;
    }

    public bool UpdateForViewChange()
    {
        if (MapView.instance.BlockOutsideViewData(x,z)) {
            return true;
        }

        return false;
    }

    public void UpdateScreenPosition()
    {
        //// Position this DisplayAttack's pivot at the screen location of the attack's target block.
        //transform.position = MapView.instance.GetBlockCenterScreenPos(x, z);

        Vector3 block_screen_pos = MapView.instance.GetBlockCenterScreenPos(x, z);

        // Move the pivot to the top or bottom, as appropriate. Set the pivot before setting the position, as it seems to reset the position.
        gameObject.GetComponent<RectTransform>().pivot = new Vector2(0.5f, (default_position == Position.BELOW) ? 1f : 0f);

        //transform.position = new Vector3(block_screen_pos.x, (default_position == Position.ABOVE) ? top_anchor_y : bottom_anchor_y);
        gameObject.GetComponent<RectTransform>().position = new Vector3(block_screen_pos.x, (default_position == Position.ABOVE) ? top_anchor_y : bottom_anchor_y);
        
        if (default_position == Position.ABOVE)
        {
            bool show_down_arrow = (block_screen_pos.y <= top_anchor_y);
            upArrowOnObject.SetActive(false);
            upArrowOffObject.SetActive(true);
            downArrowOnObject.SetActive(show_down_arrow);
            downArrowOffObject.SetActive(!show_down_arrow);
            upIndicator.gameObject.SetActive(false);
            downIndicator.gameObject.SetActive(show_down_arrow);
            if (show_down_arrow) downIndicator.sizeDelta = new Vector2(downIndicator.sizeDelta.x, (top_anchor_y - block_screen_pos.y) / TARGET_RESOLUTION_SCALE / MapView.instance.canvas.scaleFactor);
        }
        else
        {
            bool show_up_arrow = (block_screen_pos.y >= bottom_anchor_y);
            upArrowOnObject.SetActive(show_up_arrow);
            upArrowOffObject.SetActive(!show_up_arrow);
            downArrowOnObject.SetActive(false);
            downArrowOffObject.SetActive(true);
            upIndicator.gameObject.SetActive(show_up_arrow);
            downIndicator.gameObject.SetActive(false);
            if (show_up_arrow) upIndicator.sizeDelta = new Vector2(upIndicator.sizeDelta.x, (block_screen_pos.y - bottom_anchor_y) / TARGET_RESOLUTION_SCALE / MapView.instance.canvas.scaleFactor);
        }
        
        /*
        // Determine the target screen location's vertical position relative to the camera view.
        float vertical_position = (transform.position.y - (MapView.instance.camera.rect.y * Screen.height)) / (MapView.instance.camera.rect.height * Screen.height);

        // Determine this DisplayAttack's position. If it's in the bottom portion of the view, have it appear above the target. 
        // If in the top portion of the view, have it appear below the target. If in the center portion of the view, use default position.
        Position new_position = (vertical_position < 0.25) ? Position.ABOVE : ((vertical_position > 0.75) ? Position.BELOW : default_position); 

        // If this DisplayAttack's position is changing...
        if (position != new_position)
        {
            // Record the new position.
            position = new_position;

            // Move the pivot to the top or bottom, as appropriate.
            gameObject.GetComponent<RectTransform>().pivot = new Vector2(0.5f, (position == Position.BELOW) ? 1f : 0f);

            // Show the appropriate arrow.
            upArrowOnObject.SetActive(position == Position.BELOW);
            upArrowOffObject.SetActive(position == Position.ABOVE);
            downArrowOnObject.SetActive(position == Position.ABOVE);
            downArrowOffObject.SetActive(position == Position.BELOW);
        }
        */
    }

    private void SetHitPointsValues(float _attacker_value, float _defender_value)
    {
        float attacker_degree = _attacker_value / attack_points_max;
        float defender_degree = _defender_value / defend_points_max;

        // Set the sliders to the correct value
        attack_points_slider.value = attacker_degree;
        defend_points_slider.value = defender_degree;

        // Set the sliders' fill colors according to the new values
        if (attacker_degree < 0.5f) {
            attack_points_fill.color = Color.Lerp(empty_color, half_color, attacker_degree / 0.5f);
        } else {
            attack_points_fill.color = Color.Lerp(half_color, full_color, (attacker_degree - 0.5f) / 0.5f);
        }

        if (defender_degree < 0.5f) {
            defend_points_fill.color = Color.Lerp(empty_color, half_color, defender_degree / 0.5f);
        } else {
            defend_points_fill.color = Color.Lerp(half_color, full_color, (defender_degree - 0.5f) / 0.5f);
        }

        //// Set the slider value texts
        //attackerValText.text = "" + (int)(_attacker_value + 0.5f);
        //defenderValText.text = "" + (int)(_defender_value + 0.5f);
    }
}
