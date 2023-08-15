using UnityEngine;
using System.Collections;

public class FadeAndDestroy : MonoBehaviour
{
    public float fadeStartTime = 0f;
    public float fadeDuration = 0f;

    private float fade_start_time;

	// Use this for initialization
	void Start ()
    {
        fade_start_time = Time.time + fadeStartTime;
	}
	
	// Update is called once per frame
	void Update ()
    {
        float cur_time = Time.time;

        if (cur_time > fade_start_time)
        {
            float alpha = Mathf.Lerp(1f, 0f, (cur_time - fade_start_time) / fadeDuration);

            if (alpha <= 0f) {
                Destroy(gameObject);
            } else {
                ApplyAlphaRecursively(gameObject, alpha);
            }
        } 
	}

    private void ApplyAlphaRecursively(GameObject _gameObject, float _alpha)
    {
        Color albedo = new Color(1f, 1f, 1f, _alpha);

        Renderer rend;
        foreach (Transform child in _gameObject.transform)
        {
            rend = child.gameObject.GetComponent<Renderer>();
            if (rend != null) 
            {
                //Debug.Log("ApplyAlphaRecursively() top: " + _gameObject.name + ", cur:" + child.gameObject.name);
                ApplyAlphaRecursively(child.gameObject, _alpha);

                rend.material.SetColor("_Color", albedo);

                if (_alpha > 0f) {
                    rend.material.EnableKeyword ("_EMISSION");
                } else {
                    rend.material.DisableKeyword ("_EMISSION");
                }
            }
        }
    }
}
