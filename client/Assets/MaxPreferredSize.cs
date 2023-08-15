using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class MaxPreferredSize : MonoBehaviour {

    public float maxWidth = 400, maxHeight = 400;

	void OnEnable()
    {
        StartCoroutine(SetMinSizes());
	}

    IEnumerator SetMinSizes()
    {
        LayoutElement layEl = GetComponent<LayoutElement>();

        // Reset min sizes to 0, so that they do not set a lower bound on the preferred sizes in the next frame.
        layEl.minWidth = 0;
        layEl.minHeight = 0;

        yield return 0;

        // Determine this object's preferred width and height.
        float pref_width = LayoutUtility.GetPreferredWidth(GetComponent<RectTransform>());
        float pref_height = LayoutUtility.GetPreferredHeight(GetComponent<RectTransform>());

        // Set this object's min width/height to the smaller of its preferred width/height, and the given max width/height.
        layEl.minWidth = Mathf.Min(pref_width, maxWidth);
        layEl.minHeight = Mathf.Min(pref_height, maxHeight);

        Debug.Log("pref_width: " + pref_width + ", maxWidth: " + maxWidth + ", minWidth: " + layEl.minWidth);
    }
}
