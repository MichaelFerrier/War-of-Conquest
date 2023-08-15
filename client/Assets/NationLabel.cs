using UnityEngine;
using System.Collections;

public class NationLabel : MonoBehaviour
{
    public int nationID = -1;
    public TextMesh text;

    public void SetText(string _text)
    {
        text.text = _text;
    }

    public void SetAlpha(float _alpha)
    {
        text.color = new Color(text.color.r, text.color.g, text.color.b, _alpha);
    }
}
