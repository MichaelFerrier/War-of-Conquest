using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class AlertImage : MonoBehaviour
{
    public float speed = 3;

    Image image;
    float enabledTime;

	void OnEnable()
    {
        image = gameObject.GetComponent<Image>();
        enabledTime = Time.unscaledTime;
        image.color = new Color(1, 1, 1, 0);
	}
	
	void Update ()
    {
        if (image != null) {
            image.color = new Color(1, 1, 1, (Mathf.Sin(((Time.unscaledTime - enabledTime) * speed) - (Mathf.PI / 2)) + 1f) / 2f);
        }		
	}
}
