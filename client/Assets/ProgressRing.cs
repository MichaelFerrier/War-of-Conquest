using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class ProgressRing : MonoBehaviour
{
    const float FILL_DURATON = 1f;

    Image ringImage;
    float fillStartTime;
     
    void OnEnable()
    {
        ringImage = gameObject.GetComponent<Image>();
        ringImage.fillAmount = 0;
        fillStartTime = Time.unscaledTime;

        StartCoroutine(FillRing());
	}
	
	public IEnumerator FillRing()
    {
        float elapsed_time;

        while ((elapsed_time = (Time.unscaledTime - fillStartTime)) <= FILL_DURATON)
        {
            ringImage.fillAmount = elapsed_time / FILL_DURATON;
            yield return 0;
        }

        ringImage.fillAmount = 1;
    }
}
