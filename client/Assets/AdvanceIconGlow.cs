using UnityEngine;
using System.Collections;

public class AdvanceIconGlow : MonoBehaviour
{
    public RectTransform researchableGlow1_RectTransform, researchableGlow2_RectTransform, purchasableGlow1_RectTransform, purchasableGlow2_RectTransform;
    public Type type;
    private RectTransform sprite1_RectTransform = null, sprite2_RectTransform = null;
    private AdvanceIcon icon = null;
    private float animated_scale, rotation_offset;

    // Scale animation coroutine values
    float start_scale, end_scale;
    bool release_at_end;

    private const float SCALE_ANIMATION_DURATION = 1f;

	public enum Type
    {
        RESEARCHABLE,
        PURCHASABLE
    }

	// Update is called once per frame
	void Update ()
    {
        // Set the glow to use the same scale as the associated icon, and update the rotations of the glow's two sprites.
        gameObject.transform.localScale = icon.gameObject.transform.localScale * animated_scale;
        sprite1_RectTransform.Rotate(Vector3.forward * Time.unscaledDeltaTime * Mathf.Abs(Mathf.Sin((Time.unscaledTime + rotation_offset) / 4)) * 6);
        sprite2_RectTransform.Rotate(Vector3.forward * Time.unscaledDeltaTime * -1.1f);
	}

    public void Activate(AdvanceIcon _icon, Type _type)
    {
        icon = _icon;
        type = _type;

        // Set the glow to the same position as its associated icon.
        gameObject.GetComponent<RectTransform>().anchoredPosition = _icon.gameObject.GetComponent<RectTransform>().anchoredPosition;

        researchableGlow1_RectTransform = gameObject.transform.GetChild(0).GetComponent<RectTransform>();
        researchableGlow2_RectTransform = gameObject.transform.GetChild(1).GetComponent<RectTransform>();
        purchasableGlow1_RectTransform = gameObject.transform.GetChild(2).GetComponent<RectTransform>();
        purchasableGlow2_RectTransform = gameObject.transform.GetChild(3).GetComponent<RectTransform>();

        // Determine which two sprites to show.
        researchableGlow1_RectTransform.gameObject.SetActive(type == Type.RESEARCHABLE);
        researchableGlow2_RectTransform.gameObject.SetActive(type == Type.RESEARCHABLE);
        purchasableGlow1_RectTransform.gameObject.SetActive(type == Type.PURCHASABLE);
        purchasableGlow2_RectTransform.gameObject.SetActive(type == Type.PURCHASABLE);

        if (type == Type.PURCHASABLE)
        {
            sprite1_RectTransform = purchasableGlow1_RectTransform;
            sprite2_RectTransform = purchasableGlow2_RectTransform;
        }
        else 
        {
            sprite1_RectTransform = researchableGlow1_RectTransform;
            sprite2_RectTransform = researchableGlow2_RectTransform;
        }

        if (isActiveAndEnabled) {
            Grow();
        } else {
            animated_scale = 1f;
        }

        // Set the scale for the current frame.
        gameObject.transform.localScale = icon.gameObject.transform.localScale * animated_scale;

        // Set random starting rotations
        sprite1_RectTransform.Rotate(new Vector3(0, 0, Random.value * 360f));
        sprite2_RectTransform.Rotate(new Vector3(0, 0, Random.value * 360f));

        rotation_offset = Random.value * 100f;
    }

    public void Deactivate()
    {
        if (isActiveAndEnabled) {
            Shrink();
        } else {
            Release();
        }

        // Set the scale for the current frame.
        gameObject.transform.localScale = icon.gameObject.transform.localScale * animated_scale;
    }

    public void Grow()
    {
        start_scale = 0f;
        end_scale = 1f;
        release_at_end = false;
        StartCoroutine("AnimateScale");
    }

    public void Shrink()
    {
        start_scale = 1f;
        end_scale = 0f;
        release_at_end = true;
        StartCoroutine("AnimateScale");
    }

    public IEnumerator AnimateScale()
    {
        animated_scale = start_scale;

        float start_time = Time.unscaledTime;
        float end_time = start_time + SCALE_ANIMATION_DURATION;

        // Interpolate scale over time.
        while (Time.unscaledTime <= end_time) 
        {
            animated_scale = Mathf.SmoothStep(start_scale, end_scale, (Time.unscaledTime - start_time) / (end_time - start_time));
            yield return null;
        }

        // Set final scale
        animated_scale = end_scale;

        if (release_at_end) {
            Release();
        }
    }

    public void Release()
    {
        gameObject.transform.SetParent(null);
        MemManager.instance.ReleaseAdvanceIconGlowObject(gameObject);
    }
}
