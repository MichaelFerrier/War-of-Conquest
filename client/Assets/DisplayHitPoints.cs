using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class DisplayHitPoints : MonoBehaviour
{
    public enum TransitionType
    {
        LINEAR,
        EASE_IN,
        EASE_OUT,
        RANDOM
    }

    public static ArrayList display_hit_points_list = new ArrayList();

    public CanvasGroup canvasGroup;

    // The scale of this GUI object at the canvas scaler's target resolution. It will be automatically scaled from there according to the actual resolution.
    private const float TARGET_RESOLUTION_SCALE = 0.6f;

    public const float FADE_DURATION = 0.5f;

    public GameObject battleBackground;
    public Slider hit_points_slider;
    public Image hit_points_fill, hit_points_background;

    private int x, z;
    private float duration, start_time, end_time, finished_time, hit_points_start, hit_points_end, hit_points_full, hit_points_dif, hit_points_rate;
    private TransitionType transition_type;

    private Color full_color = Color.green;
    private Color half_color = Color.yellow;
    private Color empty_color = Color.red;
    private Color slider_background_color = new Color(0.6f, 0.6f, 0.6f);

    private RandomProgress random_progress = new RandomProgress();

    public static GameObject ActivateNew(int _x, int _z, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _duration, TransitionType _transition_type, bool _fade_out, bool _battle)
    {
        //Debug.Log("DisplayHitPoints.ActivateNew() _x: " + _x + ", _z: " + _z + ", _hit_points_start: " + _hit_points_start + ", _hit_points_end: " + _hit_points_end + ", _hit_points_full: " + _hit_points_full + ", _duration: " + _duration + ", _transition_type: " + _transition_type + ", _battle: " + _battle);

        GameObject display_object = MemManager.instance.GetDisplayHitPointsObject();
        display_hit_points_list.Add(display_object);

        // Init and activate the DisplayHitPoints.
        DisplayHitPoints display = display_object.GetComponent<DisplayHitPoints>();
        display.Init();
        display.Activate(_x, _z, _hit_points_start, _hit_points_end, _hit_points_full, _duration, _transition_type, _fade_out, _battle);

        return display_object;
    }

    public static void Deactivate(GameObject _display_object)
    {
        DisplayHitPoints display = _display_object.GetComponent<DisplayHitPoints>();

        // Deactivate this DisplayHitPoints
        display.Deactivate();

        // Free this DisplayHitPoints
        display_hit_points_list.Remove(_display_object);
        MemManager.instance.ReleaseDisplayHitPointsObject(_display_object);
    }

    public static void UpdateAllActivity()
    {
        // Update DisplayHitPoints objects.
        GameObject display_object;
        DisplayHitPoints display;

        // Iterate through all active DisplayHitPoints objects...
        for (int i = 0; i < display_hit_points_list.Count; i++)
        {
            // Get the current active DisplayHitPoints
            display_object = (GameObject)(display_hit_points_list[i]);
            display = display_object.GetComponent<DisplayHitPoints>();

            // Update the current active DisplayHitPoints and determine whether it's finished.
            if (display.UpdateActivity() == true)
            {
                // This DisplayHitPoints is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllForMapAreaChange()
    {
        GameObject display_object;
        DisplayHitPoints display;

        // Iterate through all active DisplayHitPoints objects...
        for (int i = 0; i < display_hit_points_list.Count; i++)
        {
            // Get the current active DisplayHitPoints
            display_object = (GameObject)(display_hit_points_list[i]);
            display = display_object.GetComponent<DisplayHitPoints>();

            // Update the current active DisplayHitPoints and determine whether it's finished.
            if (display.UpdateForViewChange() == true)
            {
                // This DisplayHitPoints is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllScreenPosition()
    {
        DisplayHitPoints display;

        // Update the screen positions of all DisplayHitPoints objects in use.
        for (int i = 0; i < display_hit_points_list.Count; i++)
        {
            GameObject display_object = (GameObject)(display_hit_points_list[i]);
            display = display_object.GetComponent<DisplayHitPoints>();
            display.UpdateScreenPosition();
        }
    }

    public static void CancelForBlock(int _x, int _z)
    {
        GameObject display_object;
        DisplayHitPoints display;

        // Iterate through all active DisplayHitPoints objects...
        for (int i = 0; i < display_hit_points_list.Count; i++)
        {
            // Get the current active DisplayHitPoints
            display_object = (GameObject)(display_hit_points_list[i]);
            display = display_object.GetComponent<DisplayHitPoints>();

            // If the current active DisplayHitPoints is for the given block, cancel it.
            if ((display.x == _x) && (display.z == _z))
            {
                Deactivate(display_object);
                i--;
            }
        }
    }

    void Start()
    {
    }

    public void Init()
    {
        hit_points_background.color = slider_background_color;
    }

    public void Activate(int _x, int _z, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _duration, TransitionType _transition_type, bool _fade_out, bool _battle)
    {
        x = _x;
        z = _z;
        hit_points_start = _hit_points_start;
        hit_points_end = _hit_points_end;
        hit_points_full = _hit_points_full;
        hit_points_dif = hit_points_end - hit_points_start;
        duration = _duration;
        transition_type = _transition_type;
        //Debug.Log("DisplayHitPoints hit_points_dif: " + hit_points_dif + ", duration: " + duration);
        start_time = Time.time;
        end_time = start_time + duration;
        finished_time = end_time + (_fade_out ? FADE_DURATION : 0);

        // Turn n the battleBackground only if appropriate.
        battleBackground.SetActive(_battle);

        // Initialize the random_progress object if appropriate.
        if (transition_type ==TransitionType.RANDOM) {
            random_progress.Init();
        }

        // Parent this DisplayHitPoints to the overlay panel.
        transform.SetParent(MapView.instance.overlay_panel_1_rect_transform);
        transform.SetAsFirstSibling();

        // Set initial scale for the canvas scaler's target resolution. Needs to be done each time it's activated, in case it was changed last time used.
        //transform.localScale = new Vector3(TARGET_RESOLUTION_SCALE, TARGET_RESOLUTION_SCALE, 1.0f);
        transform.localScale = new Vector3(1,1,1);

        // Initial alpha value to be fully visible.
        canvasGroup.alpha = 1.0f;

        // Set the initial hit points slider value.
        SetHitPointsValue(hit_points_start / hit_points_full);

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
            // Make sure the hit points slider is displaying the final value.
            hit_points_slider.value = (float)hit_points_end / (float)hit_points_full;

            // The process has ended, now fading out the DisplayHitPoints.
            canvasGroup.alpha = 1.0f - ((time - end_time) / FADE_DURATION);
        }
        else
        {
            // Set the new hit points slider value.
            float progress = (time - start_time) / duration;

            if (transition_type == TransitionType.EASE_IN) {
                progress = progress * progress;
            } else if (transition_type == TransitionType.EASE_OUT) {
                progress = 1f - ((1f - progress) * (1f - progress));
            } else if (transition_type == TransitionType.RANDOM) {
                progress = random_progress.GetRandomProgress(progress);
            }

            SetHitPointsValue((progress * hit_points_dif + hit_points_start) / hit_points_full);
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
        transform.position = MapView.instance.GetBlockCenterScreenPos(x, z);
    }

    private void SetHitPointsValue(float _value)
    {
        // Set the slider to the correct value
        hit_points_slider.value = _value;

        // Set the slider fill color according to the new value
        if (_value < 0.5f)
        {
            hit_points_fill.color = Color.Lerp(empty_color, half_color, _value / 0.5f);
        }
        else
        {
            hit_points_fill.color = Color.Lerp(half_color, full_color, (_value - 0.5f) / 0.5f);
        }
    }
}
