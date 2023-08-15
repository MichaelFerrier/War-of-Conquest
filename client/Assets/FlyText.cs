using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class FlyText : MonoBehaviour
{
    RectTransform rectTransform;
    TMPro.TextMeshProUGUI text;
    Vector2 start_pos, end_pos;
    float duration;
    bool animating = false;

    public static void DisplayFlyText(string _text, Transform _parent, Vector2 _start_pos, Vector2 _end_pos, float _duration=2f)
    {
        GameObject fly_text_object= MemManager.instance.GetFlyTextObject();
        FlyText fly_text = fly_text_object.GetComponent<FlyText>();

        fly_text.Activate(_text, _parent, _start_pos, _end_pos, _duration);
    }

    public void Activate(string _text, Transform _parent, Vector2 _start_pos, Vector2 _end_pos, float _duration)
    {
        //Debug.Log("FlyText.Activate() _parent: " + _parent + ", _start_pos: " + _start_pos + ", _end_pos: " + _end_pos+ ", _text: " + _text + ", _duration: " + _duration);

        // Do nothing if the object to parent the AnimText to is inactive in the hierarchy.
        if (_parent.gameObject.activeInHierarchy == false) {
            Debug.Log("INACTIVE");
            return;
        }

        start_pos = _start_pos;
        end_pos = _end_pos;
        duration = _duration;

        rectTransform = gameObject.GetComponent<RectTransform>();
        text = gameObject.GetComponent<TMPro.TextMeshProUGUI>();

        gameObject.transform.localScale = new Vector3(1, 1, 1);
        gameObject.transform.SetParent(_parent);
        gameObject.transform.SetAsLastSibling();
        rectTransform.pivot = new Vector2(0.5f, 0.5f);
        gameObject.transform.localPosition = new Vector3(0, 0, 0);
        gameObject.SetActive(true);

        // Set text
        text.text = _text;

        // Set the anchors.
        rectTransform.anchorMin = new Vector2(0,0);
        rectTransform.anchorMax = new Vector2(0,0);

        animating = true;

        StartCoroutine(Animate());
    }

    public void Deactivate()
    {
        // Record that we are done animating.
        animating = false;

        // Remove and release this FlyText.
        gameObject.transform.SetParent(null);
        MemManager.instance.ReleaseFlyTextObject(gameObject);
    }

    public void OnEnable()
    {
        // If this FlyText was disabled and now re-enabled while animating, then its Animate() coroutine was halted. Deactivate.
        if (animating) {
            Deactivate();
        }
    }

    public IEnumerator Animate()
    {
        float anim_start_time = Time.unscaledTime;
        float anim_end_time = anim_start_time + duration;
        float cur_degree, cur_curve_degree;

        float start_x = start_pos.x;
        float start_y = start_pos.y;
        float end_x = end_pos.x;
        float end_y = end_pos.y;

        Vector2 cur_pos;

        // Determine direction and amount by which to curve the trajectory.
        Vector2 curve = new Vector2(0, 0);
        if (Mathf.Abs(start_x - end_x) >= Mathf.Abs(start_y - end_y)) {
            curve = new Vector2(0f, -Mathf.Abs(start_x - end_x) * 0.1f);
        } else {
            curve = new Vector2(Mathf.Abs(start_y - end_y) * 0.1f, 0f);
        }
        
        // Interpolate value over time.
        while (Time.unscaledTime <= anim_end_time) 
        {
            cur_degree = (Time.unscaledTime - anim_start_time) / duration;
            cur_degree = 1f - ((1f - cur_degree) * (1f - cur_degree));
            cur_curve_degree = Mathf.Sin(cur_degree * Mathf.PI);

            // Determine degree of transparency.
            float alpha = 1f;
            if (cur_degree < 0.1f) {
                alpha = cur_degree / 0.1f;
            } else if (cur_degree > 0.9f) {
                alpha = (1f - cur_degree) / 0.1f;
            }

            // Set degree of transparency.
            text.color = new Color(text.color.r, text.color.g, text.color.b, alpha);

            // Set position, with curve.
            cur_pos = new Vector2(Mathf.Lerp(start_x, end_x, cur_degree), Mathf.Lerp(start_y, end_y, cur_degree));
            cur_pos += (curve * cur_curve_degree);
            rectTransform.anchoredPosition = cur_pos;

            yield return null;
        }

        Deactivate();
    }
}
