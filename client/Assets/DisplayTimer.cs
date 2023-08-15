using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class DisplayTimer : MonoBehaviour
{
    public static ArrayList display_timer_list = new ArrayList();
    private static float fullAlpha = 1f;

    public const float FADE_DURATION = 0.5f;

    public enum Type
    {
        ACTIVATE,
        CRUMBLE
    }

    public CanvasGroup canvasGroup;
    public Image background, fill;
    public Sprite fillActivate, fillCrumble;

    private int x, z;
    private float duration, start_time, end_time, finished_time;
    private Type type;
    private float alpha;
    
    public static DisplayTimer ActivateNew(int _x, int _z, Type _type, float _start_time, float _end_time)
    {
        GameObject display_object = MemManager.instance.GetDisplayTimerObject();
        display_timer_list.Add(display_object);

        // Init and activate the DisplayTimer.
        DisplayTimer display = display_object.GetComponent<DisplayTimer>();
        display.Activate(_x, _z, _type, _start_time, _end_time);

        return display;
    }

    public static void Deactivate(GameObject _display_object)
    {
        DisplayTimer display = _display_object.GetComponent<DisplayTimer>();

        // Deactivate this DisplayTimer
        display.Deactivate();

        // Free this DisplayTimer
        display_timer_list.Remove(_display_object);
        MemManager.instance.ReleaseDisplayTimerObject(_display_object);
    }

    public static bool IsActive(GameObject _display_object)
    {
        return (display_timer_list.IndexOf(_display_object) != -1);
    }

    public static void SetFullAlpha(float _fullAlpha)
    {
        GameObject display_object;
        DisplayTimer display;

        // Record new fullAlpa value
        fullAlpha = _fullAlpha;

        // Iterate through all active DisplayTimer objects...
        for (int i = 0; i < display_timer_list.Count; i++)
        {
            // Get the current active DisplayTimer
            display_object = (GameObject)(display_timer_list[i]);
            display = display_object.GetComponent<DisplayTimer>();

            // Redetermine the current DisplayTimer's alpha.
            display.DetermineAlpha();
        }
    }

    public static void UpdateAllActivity()
    {
        // Update DisplayTimer objects.
        GameObject display_object;
        DisplayTimer display;

        // Iterate through all active DisplayTimer objects...
        for (int i = 0; i < display_timer_list.Count; i++)
        {
            // Get the current active DisplayTimer
            display_object = (GameObject)(display_timer_list[i]);
            display = display_object.GetComponent<DisplayTimer>();

            // Update the current active DisplayTimer and determine whether it's finished.
            if (display.UpdateActivity() == true)
            {
                // This DisplayTimer is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllForMapAreaChange()
    {
        GameObject display_object;
        DisplayTimer display;

        // Iterate through all active DisplayTimer objects...
        for (int i = 0; i < display_timer_list.Count; i++)
        {
            // Get the current active DisplayTimer
            display_object = (GameObject)(display_timer_list[i]);
            display = display_object.GetComponent<DisplayTimer>();

            // Update the current active DisplayTimer and determine whether it's finished.
            if (display.UpdateForViewChange() == true)
            {
                // This DisplayTimer is finished.
                Deactivate(display_object);
                i--;
            }
        }
    }

    public static void UpdateAllScreenPosition()
    {
        DisplayTimer display;

        // Update the screen positions of all DisplayTimer objects in use.
        for (int i = 0; i < display_timer_list.Count; i++)
        {
            GameObject display_object = (GameObject)(display_timer_list[i]);
            display = display_object.GetComponent<DisplayTimer>();
            display.UpdateScreenPosition();
        }
    }

    public static void CancelForBlock(int _x, int _z)
    {
        GameObject display_object;
        DisplayTimer display;

        // Iterate through all active DisplayTimer objects...
        for (int i = 0; i < display_timer_list.Count; i++)
        {
            // Get the current active DisplayTimer
            display_object = (GameObject)(display_timer_list[i]);
            display = display_object.GetComponent<DisplayTimer>();

            // If the current active DisplayTimer is for the given block, cancel it.
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

    public void Activate(int _x, int _z, Type _type, float _start_time, float _end_time)
    {
        x = _x;
        z = _z;

        // Parent this DisplayTimer to the overlay panel.
        transform.SetParent(MapView.instance.overlay_panel_1_rect_transform);

        // Set initial scale for the canvas scaler's target resolution. Needs to be done each time it's activated, in case it was changed last time used.
        transform.localScale = new Vector3(1,1,1);

        // Determine which fill sprite to use based on type.
        SetType(_type);

        // Record the given times and set fill amount appropriately.
        SetTimes(_start_time, _end_time);

        // Have it start invisible before fading in.
        alpha = 0f;
        DetermineAlpha();

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
            // Make sure the fill image is displaying the final value.
            fill.fillAmount = 1f;

            // The process has ended, now fading out the DisplayTimer.
            alpha = 1.0f - ((time - end_time) / FADE_DURATION);
            DetermineAlpha();
        }
        else if (time < (start_time + FADE_DURATION))
        {
            // Fade in.
            alpha = ((time - start_time) / FADE_DURATION);
            DetermineAlpha();
        }
        else
        {
            // Full opacity
            if (alpha != 1f)
            {
                alpha = 1f;
                DetermineAlpha();
            }
            
            // Set the new fill image value.
            fill.fillAmount = (time - start_time) / duration;
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

    public void SetType(Type _type)
    {
        type = _type;

        // Determine which fill sprite to use.
        fill.sprite = (type == Type.ACTIVATE) ? fillActivate : fillCrumble;

        // Set sorting order for timer. Crumble timer is placed in front, taking priority over activation (cooldown/rebuild) timer.
        if (type == Type.CRUMBLE) {
            transform.SetAsLastSibling(); // Crumble timer appears in front of activate timer.
        } else {
            transform.SetAsFirstSibling(); // Activate timer should be hidden by crumble timer.
        }
    }

    public void SetTimes(float _start_time, float _end_time)
    {
        start_time = _start_time;
        end_time = _end_time;
        duration = end_time - start_time;
        finished_time = end_time + FADE_DURATION;

        // Set the fill value.
        SetValue((Time.time - start_time) / duration);
    }

    private void SetValue(float _value)
    {
        // Set the fill image fill level to the correct value
        fill.fillAmount = _value;
    }

    private void DetermineAlpha()
    {
        canvasGroup.alpha = alpha * fullAlpha;
    }
}
