using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class UIGlow : MonoBehaviour
{
    public Color color = new Color(1,1,1,1);
    public float glow1Speed = -0.1f, glow2Speed = 0.14f;

    [HideInInspector]
    public RectTransform glow1_RectTransform, glow2_RectTransform;
	
	void Update ()
    {
        glow1_RectTransform.Rotate(Vector3.forward * Time.unscaledDeltaTime * Mathf.Abs(Mathf.Sin(Time.unscaledTime / 4 + 0.5f)) * glow1Speed);
        glow2_RectTransform.Rotate(Vector3.forward * Time.unscaledDeltaTime * glow2Speed);
	}

    void OnAwake()
    {
        glow1_RectTransform = gameObject.transform.GetChild(0).GetComponent<RectTransform>();
        glow2_RectTransform = gameObject.transform.GetChild(1).GetComponent<RectTransform>();
        glow1_RectTransform.gameObject.GetComponent<Image>().color = color;
        glow2_RectTransform.gameObject.GetComponent<Image>().color = color;
    }
}
