using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class CreditsScreen : MonoBehaviour
{
    public ScrollRect scrollRect;
    public RectTransform contents;
    bool initialized = false;
    float cur_position = 1f;
    float duration = 0f;
    float scroll_speed = 0f;

    public void OnEnable()
    {
        // Duration of the scroll will be the length of the music
        duration = Sound.instance.musicCredits.length;

        // Determine scroll speed based on duration
        scroll_speed = 1 / duration;

        if (!initialized) {
            scrollRect.verticalNormalizedPosition = cur_position = 1f;
        }

        initialized = true;
        gameObject.GetComponent<GUITransition>().StartTransition(0f, 1f, 1f, 1f, false);

        // Pause the game camera while credits are shown
        MapView.instance.PauseCamera(true);

        // Play the credits music. Fade in if we're not starting at the beginning, and start at the time corresponding to the current position in the credits.
        Sound.instance.PlayMusic(Sound.instance.musicCredits, false, 0f, -1, (cur_position == 1f) ? 0f : 0.5f, 0f, (1f - cur_position) * duration);
    }

    // Update is called once per frame
    void Update ()
    {
        if (cur_position > 0)
        {
            cur_position = Mathf.Max(0f, cur_position - (scroll_speed * Time.unscaledDeltaTime));
            scrollRect.verticalNormalizedPosition = cur_position;
        
            if (cur_position == 0f) 
            {
                initialized = false;
                Hide();
            }
        }
	}

    public void Hide()
    {
        gameObject.GetComponent<GUITransition>().StartTransition(1f, 0f, 1f, 1f, true);

        // Restart game camera
        MapView.instance.ResumeCamera();

        // Fade and end the music
        Sound.instance.FadeAndEndMusic();
    }
}
