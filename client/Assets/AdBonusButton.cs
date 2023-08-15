using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class AdBonusButton : MonoBehaviour
{
    public static AdBonusButton instance;

    public RectTransform glow, button, creditsBonusAnchor, levelBonusAnchor, raidBonusAnchor;
    public TMPro.TextMeshProUGUI amountText, arrowText;
    public GUITransition buttonTransition, glowTransition;

    private RectTransform questAnchorBonus = null;
    private int totalAmount = 0;

    public enum AdBonusType
    {
        NONE      = -1,
        RESOURCE  = 0,
        ORB       = 1,
        LEVEL     = 2,
        QUEST     = 3,
        RAID      = 4,
		BLOCKS    = 5
    };

    void Awake()
    {
		instance = this;
    }

    public void Initialize(int _amount)
    {
		if (_amount == 0)
		{
			// Hide the button and glow.
			button.gameObject.SetActive(false);
			glow.gameObject.SetActive(false);
		}
		else
		{
		    // Show the button and glow.
	        button.gameObject.SetActive(true);
		    glow.gameObject.SetActive(true);
			glowTransition.SetState(1.0f, 1.0f);
			buttonTransition.SetState(1.0f, 1.0f);

			// Set the amount text.
			amountText.text = "+" + _amount + "<sprite=2>";

			// Record the current total amount.
			totalAmount = _amount;
		}
    }

    public void ModifyAmount(int _delta_amount, int _total_amount, AdBonusType _type, int _x, int _z)
    {
        //GameGUI.instance.LogToChat("AdBonusButton ModifyAmount() for _delta_amount " + _delta_amount + ", _total_amount: " + _total_amount);

        int prevTotalAmount = totalAmount;

        totalAmount = _total_amount;
        bool appearing = false;

        // Have the button appear or disappear if appropriate.
        if ((totalAmount > 0) && (prevTotalAmount == 0)) {
            StartCoroutine(Appear());
            appearing = true;
        } else if ((totalAmount == 0) && (prevTotalAmount > 0)) {
            StartCoroutine(Disappear(1.5f));
        }

        // Play sound
        Sound.instance.Play2D(Sound.instance.ad_bonus, 0.75f);

        // Gradually transition the button's amount text.
        StartCoroutine(TransitionAmount(prevTotalAmount, totalAmount, 1.5f, appearing ? 0.3f : 0.1f));

        // Determine the screen position of the AdBonusButton.
        Vector2 adButtonPos = RectTransformUtility.WorldToScreenPoint(null, button.transform.position) / MapView.instance.canvas.scaleFactor;

        // Determine the start and end positions for animating the moving credit symbols (and whether they should be shown at all).

        Vector2 start_pos = new Vector2(0,0), end_pos = new Vector2(0,0);
        bool animateCredits = true;

        if ((_type == AdBonusType.RESOURCE) || (_type == AdBonusType.ORB) || (_type == AdBonusType.BLOCKS)) {
            animateCredits = false;
            if ((_x != -1) && (_z != -1)) {
                Vector3 blockScreenPos = MapView.instance.GetBlockCenterScreenPos(_x, _z);
                if (blockScreenPos.z > 0) {
                    start_pos = new Vector2(blockScreenPos.x, blockScreenPos.y);
                    end_pos = adButtonPos;
                    animateCredits = true;
                }
            }
        } else if (_type == AdBonusType.LEVEL) {
            start_pos = RectTransformUtility.WorldToScreenPoint(null, levelBonusAnchor.transform.position) / MapView.instance.canvas.scaleFactor;
            end_pos = adButtonPos;
        } else if (_type == AdBonusType.QUEST) {
            if (questAnchorBonus == null) {
                animateCredits = false;
            } else {
                start_pos = RectTransformUtility.WorldToScreenPoint(null, questAnchorBonus.transform.position) / MapView.instance.canvas.scaleFactor;
            }
            end_pos = adButtonPos;
        } else if (_type == AdBonusType.RAID) {
            start_pos = RectTransformUtility.WorldToScreenPoint(null, raidBonusAnchor.transform.position) / MapView.instance.canvas.scaleFactor;
            end_pos = adButtonPos;
        } else if (_type == AdBonusType.NONE) {
            start_pos = adButtonPos;
            end_pos = RectTransformUtility.WorldToScreenPoint(null, creditsBonusAnchor.transform.position) / MapView.instance.canvas.scaleFactor;
        }

        if (animateCredits) {
            // Animate the moving credit symbols.
            StartCoroutine(AnimateCredits(prevTotalAmount, totalAmount, 1f, 0f, start_pos, end_pos));
        }
    }

    public IEnumerator Appear()
    {
        // Show the button and glow.
        button.gameObject.SetActive(true);
        glow.gameObject.SetActive(true);

        // Have the glow gradually appear.
        StartCoroutine(Appear_Glow());

        // Have the button gradually appear.
        buttonTransition.StartTransition(0f, 1f, 0.5f, 1f, false, 0, GUITransition.Type.Smooth);

        yield break;
    }

    public IEnumerator Appear_Glow()
    {
        glowTransition.StartTransition(0f, 1f, 0f, 1.5f, false, 0, GUITransition.Type.Smooth);
        yield return new WaitForSeconds(glowTransition.transitionDuraton);
        glowTransition.StartTransition(1f, 1f, 1.5f, 1f, false, 0, GUITransition.Type.Smooth);
    }

    public IEnumerator Disappear(float _delay)
    {
        yield return new WaitForSeconds(_delay);

        // Have the glow gradually disappear.
        glowTransition.StartTransition(1f, 0f, 1f, 0f, true, 0, GUITransition.Type.Linear);

        // Have the button gradually disappear.
        buttonTransition.StartTransition(1f, 0f, 1f, 0.5f, true, 0, GUITransition.Type.Smooth);
    }

    public IEnumerator TransitionAmount(int _startAmount, int _endAmount, float _duration, float _delay)
    {
        // Set the initial amount text.
        amountText.text = "+" + _startAmount + "<sprite=2>";

        yield return new WaitForSeconds(_delay);

        // Determine the number of steps.
        int steps = Mathf.Abs(_endAmount - _startAmount);

        if (steps == 1)
        {
			// Delay until the end, to match animation.
            yield return new WaitForSeconds(_duration);

            // Set the final amount text.
            amountText.text = "+" + _endAmount + "<sprite=2>";
            yield break;
        }

        // Determine the step increment and delay before each step.
        int increment = (_endAmount > _startAmount) ? 1 : -1;
        float stepDelay = _duration / (steps - 1);

        // For each step...
        for (int i = 1; i <= steps; i++)
        {
            // Delay before this step.
            yield return new WaitForSeconds(stepDelay);

            // Set the current amount text.
            amountText.text = "+" + (_startAmount + (i * increment)) + "<sprite=2>";
        }
    }

    public IEnumerator AnimateCredits(int _startAmount, int _endAmount, float _duration, float _delay, Vector2 _start_pos, Vector2 _end_pos)
    {
        yield return new WaitForSeconds(_delay);

        // Determine number of credit symbols to animate.
        int steps = Mathf.Min(10, Mathf.Abs(_endAmount - _startAmount));

        //if (steps == 1) {
        //    yield break;
        //}

        // Determine delay between launching each credit symbol.
        float stepDelay = _duration / Mathf.Max((steps - 1), 1);

        // For each step...
        for (int i = 1; i <= steps; i++)
        {
            // After delay, launch a credit symbol.
            yield return new WaitForSeconds(stepDelay);
            FlyText.DisplayFlyText("<sprite=2>", GameGUI.instance.canvasRectTransform, _start_pos, _end_pos, 1f);
        }
    }

    public void OnClick_PlayAd()
    {
        if (totalAmount > 0) 
		{
#if UNITY_ANDROID || UNITY_IOS
			GameGUI.instance.ShowRewardedVideo();
#else
			GameGUI.instance.OpenEarnCreditsPage();
#endif
        }
    }
    
    public void SetQuestAnchorBonus(RectTransform _questAnchorBonus)
    {
        questAnchorBonus = _questAnchorBonus;
    }
}
