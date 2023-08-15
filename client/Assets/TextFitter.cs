using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

public class TextFitter : MonoBehaviour
{
	private TMPro.TextMeshProUGUI textMeshPro;
	private float preferredSize;
	private bool initialized = false;
	public int maxNumLines = 1;
	public int minMargins = 0;

	public static List<TextFitter> textFitters = new List<TextFitter>();

	void Awake()
    {
		//Debug.Log("TextFitter '" + gameObject.transform.parent.name + "' Awake().");
		textFitters.Add(this);
    }

	void OnDestroy()
	{
		textFitters.Remove(this);
	}

    void OnEnable()
    {
		//Debug.Log("TextFitter '" + gameObject.transform.parent.name + "' enabled.");
        Fit();
    }

	public void Fit()
	{
		if (gameObject.activeInHierarchy) {
			// Delay until next frame before fitting, to allow UI to fully updte to its current state first.
			StartCoroutine(Fit_Coroutine());
		}
	}

	public IEnumerator Fit_Coroutine()
	{
		// Delay until next frame before fitting, to allow UI to fully updte to its current state first.
		yield return null; 

		if (!gameObject.activeInHierarchy) {
			yield break;
		}

		if (!initialized)
		{
			textMeshPro = gameObject.GetComponent<TMPro.TextMeshProUGUI>();

			if (textMeshPro != null) {
				preferredSize = textMeshPro.fontSize;
			}

			initialized = true;
		}

		if (textMeshPro != null)
		{
			RectTransform rt = GetComponent<RectTransform>();
            float allowedWidth = ((rt.rect.width - (2 * minMargins))* MapView.instance.canvas.scaleFactor);

			//Debug.Log("Object '" + gameObject.transform.parent.name + "' preferredSize: " + preferredSize + ", rt width: " + rt.rect.width + ", minMargins: " + minMargins + ", scaleFactor: " + MapView.instance.canvas.scaleFactor);
			//Debug.Log("Will fit '" + gameObject.transform.parent.name + "' , starts at " + preferredSize + ". allowedWidth: " + allowedWidth);

			textMeshPro.fontSize = preferredSize;
			textMeshPro.ForceMeshUpdate();
			TMP_TextInfo textInfo = textMeshPro.textInfo;
			float lineWidth = textInfo.lineInfo[0].lineExtents.max.x - textInfo.lineInfo[0].lineExtents.min.x;

			while ((textMeshPro.fontSize > 1) && ((textInfo.lineCount > maxNumLines) || (lineWidth > allowedWidth)))
			{
				//Debug.Log("Decreasing font size. lineCount: " + textInfo.lineCount + ", line 0 lineWidth: " + lineWidth + ", allowedWidth: " + allowedWidth + ", marginLeft: " + textInfo.lineInfo[0].marginLeft + ", lineExtent: " + textInfo.lineInfo[0].lineExtents);
				textMeshPro.fontSize--;
				textMeshPro.ForceMeshUpdate();
				textInfo = textMeshPro.textInfo;
				lineWidth = textInfo.lineInfo[0].lineExtents.max.x - textInfo.lineInfo[0].lineExtents.min.x;
			}

			//Debug.Log("Fit at size " + textMeshPro.fontSize);
		}
	}

	public static void FitAll()
	{
		foreach (TextFitter curTextFitter in textFitters) {
			if (curTextFitter.gameObject.activeSelf) {
			  curTextFitter.Fit();
			}
		}
	}
}
