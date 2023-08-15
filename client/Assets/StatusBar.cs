using UnityEngine;
using UnityEngine.UI;
using System;
using System.Collections;

public class StatusBar : MonoBehaviour
{
    public Text text;
    public Image fill_image, empty_image;
    public Sprite emptySpriteWithButton, emptySpriteWithoutButton;
    public string postfix = "";
    public bool display_max = false;

    int value = 0, max_value = 0;
    float anim_start_time, anim_end_time;
    bool visible = true;

    float ANIMATION_DURATION = 0.5f;

    public void OnDisable()
    {
        // If currently animating, set state to end state of animation, since being disabled will end the animaton coroutine.
        if (anim_end_time > Time.unscaledTime)
        {
            DisplayText(value, max_value);
            DisplayBar(value, max_value);
        }
    }

    public void SetOptions(bool _display_max, string _postfix)
    {
        display_max = _display_max;
        postfix = _postfix;
    }

    public void SetMaxValue(int _max_value)
    {
        max_value = _max_value;

        // Show the change to the new max value immediately.
        DisplayText(value, max_value);
        DisplayBar(value, max_value);
    }

    public void SetValue(int _value, bool _animate)
    {
        int anim_start_value = value;
        value = _value;

        if (_animate && gameObject.activeInHierarchy)
        {
            // Animate the change to the new value.
            StartCoroutine(Animate(anim_start_value));
        }
        else 
        {
            // Show the change to the new value immediately.
            DisplayText(value, max_value);
            DisplayBar(value, max_value); 
        }
    }

    public int GetValue()
    {
        return value;
    }
    /*
    public void SetVisibility(bool _visible, bool _animate)
    {
        if (visible == _visible) {
            return;
        }

        visible = _visible;
        if (_animate && gameObject.activeInHierarchy)
        {
            if (visible) {
                StartCoroutine(AnimateAppear());
            } else {
                StartCoroutine(AnimateDisappear());
            }
        }
        else 
        {
            gameObject.GetComponent<CanvasGroup>().alpha = visible ? 1f : 0f;
        }
    }
    */

    public void DisplayAnimText(string _text)
    {
        //Debug.Log("DisplayAnimText(" + _text + ")");

        // Do not display anim text if this status bar is not active.
        if (!gameObject.activeInHierarchy) {
            return;
        }

        GameObject anim_text_object= MemManager.instance.GetAnimTextObject();
        AnimText anim_text = anim_text_object.GetComponent<AnimText>();

        anim_text.Activate(gameObject.transform.parent, gameObject.GetComponent<RectTransform>().anchorMin, gameObject.GetComponent<RectTransform>().anchorMax, gameObject.GetComponent<RectTransform>().anchoredPosition, _text);
    }

    public IEnumerator Animate(int _anim_start_value)
    {
        anim_start_time = Time.unscaledTime;
        anim_end_time = anim_start_time + ANIMATION_DURATION;

        float value_dif = value - _anim_start_value;
        int cur_value;

        // Interpolate value over time.
        while (Time.unscaledTime <= anim_end_time) 
        {
            cur_value = Mathf.RoundToInt(((Time.unscaledTime - anim_start_time)  * value_dif / ANIMATION_DURATION) + _anim_start_value);
            DisplayText(cur_value, max_value);
            DisplayBar(cur_value, max_value);
            yield return null;
        }

        // Display final value
        DisplayText(value, max_value);
        DisplayBar(value, max_value);
    }

    public IEnumerator AnimateAppear()
    {
        anim_start_time = Time.unscaledTime;
        anim_end_time = anim_start_time + ANIMATION_DURATION;

        // Interpolate value over time.
        while (Time.unscaledTime <= anim_end_time) 
        {
            gameObject.GetComponent<CanvasGroup>().alpha = ((Time.unscaledTime - anim_start_time) / ANIMATION_DURATION);
            yield return null;
        }

        // Fully visible
        gameObject.GetComponent<CanvasGroup>().alpha = 1f;
    }

    public IEnumerator AnimateDisappear()
    {
        anim_start_time = Time.unscaledTime;
        anim_end_time = anim_start_time + ANIMATION_DURATION;

        // Interpolate value over time.
        while (Time.unscaledTime <= anim_end_time) 
        {
            gameObject.GetComponent<CanvasGroup>().alpha = 1f - ((Time.unscaledTime - anim_start_time) / ANIMATION_DURATION);
            yield return null;
        }

        // Fully invisible
        gameObject.GetComponent<CanvasGroup>().alpha = 0f;
    }

    public void DisplayText(int _value, int _max_value)
    {
        text.text = "" + String.Format("{0:n0}", _value) + (display_max ? (" / " + String.Format("{0:n0}", _max_value)) : "") + postfix;
        text.fontSize = ((_value >= 1000000) || (_max_value >= 1000000)) ? 13 : 16;
    }

    public void DisplayBar(int _value, int _max_value)
    {
        fill_image.fillAmount = (float)(Mathf.Max(Mathf.Min(_value, _max_value), 0)) / (float)(Mathf.Max(_max_value, 1));
    }

    public void SetDisplayButton(bool _display_button)
    {
        empty_image.sprite = _display_button ? emptySpriteWithButton : emptySpriteWithoutButton;
    }
}
