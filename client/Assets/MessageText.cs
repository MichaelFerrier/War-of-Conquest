using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class MessageText : MonoBehaviour
{
    public static MessageText instance;

    public GameObject messageTextObject;
    public TMPro.TextMeshProUGUI messageText;

    public float MESSAGE_TEXT_FULL_DURATION = 5.0f;
    public float MESSAGE_TEXT_FADE_DURATION = 2.0f;

    private float MESSAGE_TEXT_R = 1.0f;
    private float MESSAGE_TEXT_G = 1.0f;
    private float MESSAGE_TEXT_B = 0.6f;

    private bool active = false;
    private float fade_start_time, fade_end_time;

    public MessageText()
    {
        instance = this;
    }
	
	// Update is called once per frame
	void Update ()
    {
        if (messageTextObject.activeSelf)
        {
            float cur_time = Time.unscaledTime;

            if (cur_time >= fade_end_time)
            {
                // Deactivate the message text.
                messageTextObject.SetActive(false);
            }
            else if (cur_time > fade_start_time)
            {
                // Set alpha as appropriate for the fade.
                messageText.color = new Color(MESSAGE_TEXT_R, MESSAGE_TEXT_G, MESSAGE_TEXT_B, 1.0f - ((cur_time - fade_start_time) / (fade_end_time - fade_start_time)));
            }
        }	
	}

    public void DisplayMessage(string _message)
    {
        // Set the text of the message.
        messageText.text = _message;

        // Have the text appear with alpha=1.
        messageText.color = new Color(MESSAGE_TEXT_R, MESSAGE_TEXT_G, MESSAGE_TEXT_B, 1.0f);

        // Record times at which fade should begin and end.
        fade_start_time = Time.unscaledTime + MESSAGE_TEXT_FULL_DURATION;
        fade_end_time = Time.unscaledTime + MESSAGE_TEXT_FULL_DURATION + MESSAGE_TEXT_FADE_DURATION;

        // Activate the message text object.
        messageTextObject.SetActive(true);
    }
}
