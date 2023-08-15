using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class GUITransition : MonoBehaviour
{
    public CanvasGroup canvasGroup;
    public float transitionDuraton = 0.2f;
    public bool pickUpWhereInterrupted = false;

    float transitionStartTime = -1;
    float startAlpha, endAlpha;
    float startScale, endScale;
    bool deactivateAfterTransition = false;
    Type type;

    public enum Type
    {
        Linear,
        Smooth
    }

    void Update ()
    {
        // Update transition if necessary
        if (transitionStartTime != -1)
        {
            // If the delay before the transition starts is not complete, do nothing.
            if (Time.unscaledTime < transitionStartTime) {
                return;
            }

            if (Time.unscaledTime >= (transitionStartTime + transitionDuraton))
            {
                EndTransition();
            }
            else
            {
                float progress;
                if (type == Type.Linear) {
                    progress = Mathf.Sin((Time.unscaledTime - transitionStartTime) / transitionDuraton * (Mathf.PI / 2));
                } else {
                    progress = (Time.unscaledTime - transitionStartTime) / transitionDuraton;
                }

                if (canvasGroup != null) canvasGroup.alpha = (endAlpha - startAlpha) * progress + startAlpha;
                float curScale = (endScale - startScale) * progress + startScale;
                gameObject.transform.localScale = new Vector3(curScale, curScale, curScale);
            }
        }
	}

    public void StartTransition(float _startAlpha, float _endAlpha, float _startScale, float _endScale, bool _deactivateAfterTransition, float _delay = 0f, Type _type = Type.Linear)
    {
        if ((transitionStartTime == -1) || (!pickUpWhereInterrupted)) 
        {
            startAlpha = _startAlpha;
            startScale = _startScale;
        } else 
        {
            if (canvasGroup != null) startAlpha = canvasGroup.alpha;
            startScale = gameObject.transform.localScale.x;
        }

        endAlpha = _endAlpha;
        endScale = _endScale;
        deactivateAfterTransition = _deactivateAfterTransition;
        transitionStartTime = Time.unscaledTime + _delay;
        type = _type;

        if (canvasGroup != null) canvasGroup.alpha = startAlpha;
    }

    public void EndTransition()
    {
        if (transitionStartTime != -1)
        {
            transitionStartTime = -1;

            if (canvasGroup != null) canvasGroup.alpha = endAlpha;
            gameObject.transform.localScale = new Vector3(endScale, endScale, endScale);

            if (deactivateAfterTransition) {
                gameObject.SetActive(false);
            }
        }
    }

    public void SetState(float _alpha, float _scale)
    {
        if (canvasGroup != null) canvasGroup.alpha = _alpha;
        gameObject.transform.localScale = new Vector3(_scale, _scale, _scale);
    }

    public float GetAlpha()
    {
        return (canvasGroup != null) ? canvasGroup.alpha : 0f;
    }
}
