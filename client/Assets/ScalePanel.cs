using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ScalePanel : MonoBehaviour
{
	void OnEnable()
    {
        UpdateScale();
	}

    public void UpdateScale()
    {
        // Do not scale panels if this is not a small screen device.
        if ((GameGUI.instance == null) || (!GameGUI.instance.smallScreenDevice)) {
            return;
        }

		// Find the active child panel.
        GameObject active_panel = null;
        foreach(Transform child in gameObject.transform)
        {
            if (child.gameObject.activeInHierarchy) {
                active_panel = child.gameObject;
                break;
            }
        }

        //Debug.Log("ScalePanel: active_panel: " + active_panel);
        
        if (active_panel != null)
        {
            // Have all UIelements update their size/position now, to be used below.
            Canvas.ForceUpdateCanvases();

            //Debug.Log("gameObject rect.width: " + gameObject.GetComponent<RectTransform>().rect.width + " active_panel rect.width: " + active_panel.GetComponent<RectTransform>().rect.width + ", gameObject rect.height: " + gameObject.GetComponent<RectTransform>().rect.height + " active_panel rect.height: " + active_panel.GetComponent<RectTransform>().rect.height);

            float scale = 1f;
            if (active_panel.GetComponent<NoScalePanel>() == null) {
                scale = Mathf.Min(gameObject.GetComponent<RectTransform>().rect.width / active_panel.GetComponent<RectTransform>().rect.width, gameObject.GetComponent<RectTransform>().rect.height / active_panel.GetComponent<RectTransform>().rect.height);
            }

            active_panel.GetComponent<RectTransform>().localScale = new Vector3(scale, scale, 1);
        }
    }
}
