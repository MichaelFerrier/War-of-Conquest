using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class AnimText : MonoBehaviour
{
    RectTransform rectTransform;
    Text text;
    float delta_x, delta_y, start_scale, end_scale, start_alpha, end_alpha, duration;
    bool animating = false;

    public void Activate(Transform _parent, Vector2 _anchorMin, Vector2 _anchorMax, Vector2 _position, string _text, float _delta_x=0, float _delta_y=-20, float _start_scale=0.1f, float _end_scale=1.5f, float _start_alpha=0.8f, float _end_alpha=0f, float _duration=2f)
    {
        //Debug.Log("AnimText.Activate() _parent: " + _parent + ", _anchorMin: " + _anchorMin + ", _anchorMax: " + _anchorMax + ", _position: " + _position + ", _text: " + _text + ", _delta_x: " + _delta_x + ", _delta_y: " + _delta_y + ", _start_scale: " + _start_scale + ", _end_scale: " + _end_scale + ", _start_alpha: " + _start_alpha + ", _end_alpha: " + _end_alpha + ", _duration: " + _duration);

        // Do nothing if the object to parent the AnimText to is inactive in the hierarchy.
        if (_parent.gameObject.activeInHierarchy == false) {
            Debug.Log("INACTIVE");
            return;
        }

        delta_x = _delta_x;
        delta_y = _delta_y;
        start_scale = _start_scale;
        end_scale = _end_scale;
        start_alpha = _start_alpha;
        end_alpha = _end_alpha;
        duration = _duration;

        rectTransform = gameObject.GetComponent<RectTransform>();
        text = gameObject.GetComponent<Text>();

        gameObject.transform.localScale = new Vector3(1, 1, 1);
        gameObject.transform.SetParent(_parent);
        gameObject.transform.SetAsLastSibling();
        gameObject.SetActive(true);

        // Set text
        text.text = _text;

        // Set the anchors and position.
        rectTransform.anchorMin = _anchorMin;
        rectTransform.anchorMax = _anchorMax;
        rectTransform.anchoredPosition = _position;

        animating = true;

        StartCoroutine(Animate());
    }

    public void Deactivate()
    {
        // Record that we are done animating.
        animating = false;

        // Remove and release this AnimText.
        gameObject.transform.SetParent(null);
        MemManager.instance.ReleaseAnimTextObject(gameObject);
    }

    public void OnEnable()
    {
        // If this AnimText was disabled and now re-enabled while animating, then its Animate() coroutine was halted. Deactivate.
        if (animating) {
            Deactivate();
        }
    }

    public IEnumerator Animate()
    {
        float anim_start_time = Time.unscaledTime;
        float anim_end_time = anim_start_time + duration;
        float cur_degree;

        float start_x = rectTransform.anchoredPosition.x;
        float start_y = rectTransform.anchoredPosition.y;
        float end_x = start_x + delta_x;
        float end_y = start_y + delta_y;
        float fade_start_degree = 0.75f;

        // Interpolate value over time.
        while (Time.unscaledTime <= anim_end_time)
        {
            cur_degree = (Time.unscaledTime - anim_start_time) / duration;
            cur_degree = 1f - ((1f - cur_degree) * (1f - cur_degree));
            //Debug.Log("AnimText at " + start_x + "," + start_y + " cur_degree: " + cur_degree);

            rectTransform.anchoredPosition = new Vector2(Mathf.Lerp(start_x, end_x, cur_degree), Mathf.Lerp(start_y, end_y, cur_degree));
            text.color = (cur_degree <= fade_start_degree) ? new Color(text.color.r, text.color.g, text.color.b, start_alpha) : new Color(text.color.r, text.color.g, text.color.b, Mathf.Lerp(start_alpha, end_alpha, (cur_degree - fade_start_degree) / (1f - fade_start_degree)));
            gameObject.transform.localScale = new Vector3(Mathf.Lerp(start_scale, end_scale, cur_degree), Mathf.Lerp(start_scale, end_scale, cur_degree), Mathf.Lerp(start_scale, end_scale, cur_degree));

            yield return null;
        }

        //Debug.Log("AnimText at " + start_x + "," + start_y + " finished, being removed.");

        Deactivate();
    }
}
