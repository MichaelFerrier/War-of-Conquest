using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class SurroundCount : MonoBehaviour
{
    public TextMesh text, outline1, outline2;     

    public void SetText(string _text)
    {
        text.text = _text;
        outline1.text = _text;
        outline2.text = _text;
    }

    public void SetAlpha(float _alpha)
    {
        text.color = new Color(text.color.r, text.color.g, text.color.b, _alpha);
        outline1.color = new Color(outline1.color.r, outline1.color.g, outline1.color.b, _alpha);
        outline2.color = new Color(outline2.color.r, outline2.color.g, outline2.color.b, _alpha);
    }
}
