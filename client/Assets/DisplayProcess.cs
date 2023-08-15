using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class DisplayProcess : MonoBehaviour
{
    public static ArrayList display_process_list = new ArrayList();

    public CanvasGroup canvasGroup;

    // The scale of this GUI object at the canvas scaler's target resolution. It will be automatically scaled from there according to the actual resolution.
    private const float TARGET_RESOLUTION_SCALE = 0.20f;

    private const float FADE_DURATION = 0.5f;

    private int x, z;
    private float start_time, end_time, finished_time, duration, hit_points_start, hit_points_end, hit_points_dif, hit_points_max;
    private bool ended = false;
    public Slider hit_points_slider;
    public Image hit_points_fill, hit_points_background;

    private Color full_color = Color.green;
    private Color half_color = Color.yellow;
    private Color empty_color = Color.red;
    private Color slider_background_color = new Color(0.6f, 0.6f, 0.6f);

    public static GameObject ActivateNew(int _duration, int _x, int _z, int _hit_points_max, int _hit_points_start, int _hit_points_end)
    {
        GameObject display_object = MemManager.instance.GetDisplayProcessObject();
        display_process_list.Add(display_object);

        // Init and activate the DisplayProcess.
        DisplayProcess display = display_object.GetComponent<DisplayProcess>();
        display.Init();
        display.Activate(_duration, _x, _z, _hit_points_max, _hit_points_start, _hit_points_end);

        // Lock the block until the process is done, to prevent further clicks on it from this player.
        MapView.instance.LockBlock(_x, _z, Time.unscaledTime + _duration);

        return display_object;
    }

    private static void Deactivate(GameObject _display_object)
    {
        DisplayProcess display = _display_object.GetComponent<DisplayProcess>();

        // Deactivate this DisplayProcess
        display.Deactivate();

        // Free this DisplayProcess
        display_process_list.Remove(_display_object);
        MemManager.instance.ReleaseDisplayProcessObject(_display_object);
    }

    public static void UpdateAllActivity()
    {
        // Update DisplayProcess objects.
        GameObject display_object;
        DisplayProcess display;

        // Iterate through all active DisplayProcess objects...
        for (int i = 0; i < display_process_list.Count; i++)
        {
            // Get the current active DisplayProcess
            display_object = (GameObject)(display_process_list[i]);
            display = display_object.GetComponent<DisplayProcess>();

            // Update the current active DisplayProcess and determine whether it's finished.
            if (display.UpdateActivity() == true)
            {
                // This DisplayProcess is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllForMapAreaChange()
    {
        GameObject display_object;
        DisplayProcess display;

        // Iterate through all active DisplayProcess objects...
        for (int i = 0; i < display_process_list.Count; i++)
        {
            // Get the current active DisplayProcess
            display_object = (GameObject)(display_process_list[i]);
            display = display_object.GetComponent<DisplayProcess>();

            // Update the current active DisplayProcess and determine whether it's finished.
            if (display.UpdateForViewChange() == true)
            {
                // This DisplayProcess is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllScreenPosition()
    {
        DisplayProcess display;

        // Update the screen positions of all DisplayProcess objects in use.
        for (int i = 0; i < display_process_list.Count; i++)
        {
            GameObject display_object = (GameObject)(display_process_list[i]);
            display = display_object.GetComponent<DisplayProcess>();
            display.UpdateScreenPosition();
        }
    }

    public static void CancelForBlock(int _x, int _z)
    {
        GameObject display_object;
        DisplayProcess display;

        // Iterate through all active DisplayProcess objects...
        for (int i = 0; i < display_process_list.Count; i++)
        {
            // Get the current active DisplayProcess
            display_object = (GameObject)(display_process_list[i]);
            display = display_object.GetComponent<DisplayProcess>();

            // If the current active DisplayProcess is for the given block, cancel it.
            if ((display.x == _x) && (display.z == _z))
            {
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static int GetNumActive()
    {
        // Determine count of active DisplayProcess objects that have not ended yet (don't count those that are just fading away).
        int count = 0;
        for (int i = 0; i < display_process_list.Count; i++)
        {
            // Get the current active DisplayProcess
            GameObject display_object = (GameObject)(display_process_list[i]);
            DisplayProcess display = display_object.GetComponent<DisplayProcess>();

            if (display.end_time > Time.time) {
                count++;
            }
        }

        return count;
    }

    void Start() {
    }

    public void Init()
    {
        hit_points_background.color = slider_background_color;
    }

    public void Activate(int _duration, int _x, int _z, int _hit_points_max, int _hit_points_start, int _hit_points_end)
    {
        x = _x;
        z = _z;
        duration = (float)_duration;
        start_time = Time.time;
        end_time = start_time + duration;
        finished_time = end_time + FADE_DURATION;
        ended = false;
        hit_points_start = (float)_hit_points_start;
        hit_points_end = (float)_hit_points_end;
        hit_points_dif = hit_points_end - hit_points_start;
        hit_points_max = (float)_hit_points_max;

        // Parent this DisplayProcess to the overlay panel.
        transform.SetParent(MapView.instance.overlay_panel_2_rect_transform);

        // Have this latest display show below any older displays in this same overlay, since the other displays will disappear sooner.
        transform.SetAsFirstSibling();

        // Set initial scale for the canvas scaler's target resolution. Needs to be done each time it's activated, in case it was changed last time used.
        transform.localScale = new Vector3(TARGET_RESOLUTION_SCALE, TARGET_RESOLUTION_SCALE, 1.0f);

        // Initial alpha value.
        canvasGroup.alpha = 1f;

        // Set the initial hit points slider value.
        SetHitPointsValue(hit_points_start / hit_points_max);

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

                // Make sure the hit points slider is displaying the final value.
                hit_points_slider.value = hit_points_end / hit_points_max;

                // Update for a process having ended.
                MapView.instance.ProcessEnded();
            }

            // The process has ended, now fading out the DisplayProcess.
            canvasGroup.alpha = 1.0f - ((time - end_time) / FADE_DURATION);
        }
        else
        {
            // Set the new hit points slider value.
            float progress = (time - start_time) / duration;
            SetHitPointsValue((progress * hit_points_dif + hit_points_start) / hit_points_max);
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
        if (_value < 0.5f) {
            hit_points_fill.color = Color.Lerp(empty_color, half_color, _value / 0.5f);
        } else {
            hit_points_fill.color = Color.Lerp(half_color, full_color, (_value - 0.5f) / 0.5f);
        }
    }
}
