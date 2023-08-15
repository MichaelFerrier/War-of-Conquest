using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class Pulsate : MonoBehaviour
{
	// Update is called once per frame
	void Update () {
        gameObject.GetComponent<Image>().color = new Color(1f, 1f, 1f, Mathf.PingPong(Time.unscaledTime * 0.8f, 0.5f) + 0.5f);
	}
}
