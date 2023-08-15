using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class TempIcon : MonoBehaviour
{
    public Image backgroundImage;
    public Image fillImage;
    public Sprite bioSprite;
    public Sprite techSprite;
    public Sprite psiSprite;
    public GUITransition glowTransition, iconTransition;
    public LayoutElement layoutElement;

    public int techID;
    public TechData techData;
    public float expireTime, duration;
    public Mode mode;

    const int ICON_HEIGHT = 25;

    public enum Mode
    {
        UNDEF,
        TRANSITION_IN,
        TRANSITION_OUT
    }

    public void Init(int _techID, float _expireTime)
    {
        mode = Mode.UNDEF;

        techID = _techID;
        expireTime = _expireTime;

        techData = TechData.GetTechData(_techID);
        duration = (techData.duration_type == TechData.Duration.TEMPORARY) ? techData.duration_time : 1;

        //Debug.Log("tech " + techData.name + " ctg: " + techData.category);

        switch (techData.category)
        {
            case TechData.Category.BIO:
            case TechData.Category.BIO_BUY:
                backgroundImage.sprite = bioSprite;
                fillImage.sprite = bioSprite;
                break;
            case TechData.Category.PSI:
            case TechData.Category.PSI_BUY:
                backgroundImage.sprite = psiSprite;
                fillImage.sprite = psiSprite;
                break;
            case TechData.Category.TECH:
            case TechData.Category.TECH_BUY:
            case TechData.Category.BASE_BUY:
            default:
                backgroundImage.sprite = techSprite;
                fillImage.sprite = techSprite;
                break;
        }

        UpdateFillLevel();

        StartCoroutine(TransitionIn());
    }

    public void OnEnable()
    {
        // If disabled while transitioning in, complete transition.
        if (mode == Mode.TRANSITION_IN) {
            StartCoroutine(TransitionIn());
        }

        // If disabled while transitioning out, complete transition.
        if (mode == Mode.TRANSITION_OUT) {
            StartCoroutine(TransitionOut());
        }
    }

    public void Remove()
    {
        StartCoroutine(TransitionOut());
    }

    public void UpdateFillLevel()
    {
        float remaining_time = Mathf.Max(0, expireTime - Time.unscaledTime);
        fillImage.fillAmount = Mathf.Min(1f, remaining_time / duration);
    }

    public IEnumerator TransitionIn()
    {
        float start_time, end_time;

        mode = Mode.TRANSITION_IN;

        // Start with the icon inactive
        iconTransition.gameObject.SetActive(false);

        // Transition in the glow
        glowTransition.gameObject.SetActive(true);
        glowTransition.StartTransition(0, 1, 0, 1, false);

        // While the glow is transitioning in, increase the icon's layout height.
        start_time = Time.unscaledTime;
        end_time = start_time + 0.5f;
        while (Time.unscaledTime <= end_time)
        {
            layoutElement.minHeight = Mathf.SmoothStep(0, 1, (Time.unscaledTime - start_time) / (end_time - start_time)) * ICON_HEIGHT;
            yield return null;
        }
        layoutElement.minHeight = ICON_HEIGHT;

        // Transition in the icon 
        iconTransition.gameObject.SetActive(true);
        iconTransition.StartTransition(0, 1, 1, 1, false);

        // Transition out the glow
        glowTransition.StartTransition(1, 1, 1, 0, true);

        mode = Mode.UNDEF;
    }

    public IEnumerator TransitionOut()
    {
        float start_time, end_time;

        mode = Mode.TRANSITION_OUT;

        // Transition in the glow
        glowTransition.gameObject.SetActive(true);
        glowTransition.StartTransition(0, 1, 0, 1, false);

        // Wait until glow has transitioned in
        yield return new WaitForSeconds(0.5f);

        // Transition out the icon 
        iconTransition.StartTransition(1, 0, 1, 0, true);

        // Transition out the glow
        glowTransition.StartTransition(1, 1, 1, 0, true);

        // While the icon and glow are transitioning out, decrease the icon's layout height.
        start_time = Time.unscaledTime;
        end_time = start_time + 0.5f;
        while (Time.unscaledTime <= end_time)
        {
            layoutElement.minHeight = (1f - Mathf.SmoothStep(0, 1, ((Time.unscaledTime - start_time) / (end_time - start_time)))) * ICON_HEIGHT;
            yield return null;
        }
        layoutElement.minHeight = 0;

        mode = Mode.UNDEF;
        
        // Remove and destroy this TempIcon
        transform.SetParent(null);
        Destroy(this);
    }

    public void OnPress()
    {
        TempTechInfo.Activate(this);
    }
}
