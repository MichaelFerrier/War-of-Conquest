using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class AdminPanel : MonoBehaviour
{
    public static AdminPanel instance;

    public Text log_text;
    public InputField command_inputfield;

    public ScrollRect logScrollRect;
    public GameObject logContentObject;
    public RectTransform logScrollViewRectTransform, logContentRectTransform;

    private bool scroll_to_end_on_enable = false;

	public AdminPanel()
    {
        instance = this;
	}

    void OnEnable()
    {
        if (scroll_to_end_on_enable)
        {
            StartCoroutine(ScrollToBottomOfLog());
            scroll_to_end_on_enable = false;
        }
    }

	public void AddLogText(string _log_text)
    {
        // Determine whether we're currently scrolled to the end of the log text, and so should scroll to the new end after this is added.
        bool showing_log_end = (logScrollRect.normalizedPosition.y < 0.02f) || (logContentRectTransform.rect.height < (logScrollViewRectTransform.rect.height + 10));

        // Add the given text to the log.
        log_text.text += _log_text;

        // If the end of the log was previously being shown then scroll down to the (new) end of the log.
        // A co-routine can only be started on an active object.
        if (showing_log_end) 
        {
            if (gameObject.activeSelf) {
                StartCoroutine(ScrollToBottomOfLog());
            }  else {
                scroll_to_end_on_enable = true;
            }
        }
    }

    IEnumerator ScrollToBottomOfLog()
    {
        // Wait to do this until the end of the frame, at which point the new size of the log contents is known.
        yield return new WaitForEndOfFrame();
        logScrollRect.normalizedPosition = new Vector2(0f, 0f);
    }

    public void CommandEndEdit()
    {
        if (Input.GetButtonDown("Submit"))
        {
            CommandSubmit();
        }
    }

    void CommandSubmit()
    {
        // If there is no command text, do nothing.
        if (command_inputfield.text.Length == 0) {
            return;
        }

        // Send a command event to the server.
        Network.instance.SendCommand("action=admin_command|command=" + command_inputfield.text);

        // Clear the command input field's text.
        command_inputfield.text = "";
    }
}
