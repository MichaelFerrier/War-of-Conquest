using UnityEngine;
using System.Collections;

public class AnimTextSource : MonoBehaviour
{
    public void DisplayAnimText(string _text, float _delta_x=0, float _delta_y=-20, float _start_scale=0.1f, float _end_scale=1.5f, float _start_alpha=0.8f, float _end_alpha=0f, float _duration=2f)
    {
        GameObject anim_text_object= MemManager.instance.GetAnimTextObject();
        AnimText anim_text = anim_text_object.GetComponent<AnimText>();

        anim_text.Activate(gameObject.transform.parent, gameObject.GetComponent<RectTransform>().anchorMin, gameObject.GetComponent<RectTransform>().anchorMax, gameObject.GetComponent<RectTransform>().anchoredPosition, _text, _delta_x, _delta_y, _start_scale, _end_scale, _start_alpha, _end_alpha, _duration);
    }
}
