using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class Requestor : MonoBehaviour
{
    public enum RequestorButton
    {
        LeftButton,
        RightButton,
        CloseButton
    }

    public static int FLAG_ALIGN_LEFT = 1;
    public static int FLAG_CLOSE_BUTTON = 2;

    public static Requestor instance;

    public GameObject requestorBaseObject;
    public RectTransform requestorRectTransform;
    public TMPro.TextMeshProUGUI text;
    public GameObject middleSpacerObject, button2Object;
    public Button button1, button2, closeButton;
    public TMPro.TextMeshProUGUI buttonText1, buttonText2;

    static int task, data;
    static RequestorListener listener = null;

    public Requestor()
    {
        instance = this;
    }

    public static void Activate(int _task, int _data, RequestorListener _listener, string _text, string _left_button_text, string _right_button_text, int _flags = 0)
    {
        task = _task;
        data = _data;
        listener = _listener;

        // Set text alignment
        instance.text.alignment = ((_flags & FLAG_ALIGN_LEFT) != 0) ? TMPro.TextAlignmentOptions.MidlineLeft : TMPro.TextAlignmentOptions.Midline;
 
        // Set the text for the requestor and its two buttons.
        instance.text.text = _text;
        instance.buttonText1.text = _left_button_text;
        instance.buttonText2.text = _right_button_text;

        // If text is given for the right button, display the right button and the middle spacer. 
        instance.middleSpacerObject.SetActive((_right_button_text != null) && (_right_button_text.Length > 0));
        instance.button2Object.SetActive((_right_button_text != null) && (_right_button_text.Length > 0));

        // Set the size of the requestor, according to the length of the given text.
        Vector2 requestor_size = new Vector2(((_text == null) || (_text.Length <= 70)) ? 340 : 440, instance.requestorRectTransform.sizeDelta.y);
        instance.requestorRectTransform.sizeDelta = requestor_size;

        // Enable the buttons
        instance.button1.enabled = true;
        instance.button2.enabled = true;

        // Show the close button only if the FLAG_CLOSE_BUTTON is set.
        instance.closeButton.gameObject.SetActive((_flags & FLAG_CLOSE_BUTTON) != 0);

        // If the requestor is already previously active, make sure its scale is adjusted for the size of its new contents.
        if (instance.requestorBaseObject.activeInHierarchy) {
            instance.requestorBaseObject.GetComponent<ScalePanel>().UpdateScale();
        }

        // Show the requestor.
        instance.requestorBaseObject.SetActive(true);
        instance.requestorBaseObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
    }

    public void LeftButtonPressed()
    {
        // Disable the buttons
        instance.button1.enabled = false;
        instance.button2.enabled = false;

        // Hide the requestor
        instance.requestorBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

        if (listener != null)
        {
            // Call the response method.
            listener.RequestorResponse(task, data, RequestorButton.LeftButton);
        }
    }

    public void RightButtonPressed()
    {
        // Disable the buttons
        instance.button1.enabled = false;
        instance.button2.enabled = false;

        // Hide the requestor
        instance.requestorBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

        if (listener != null)
        {
            // Call the response method.
            listener.RequestorResponse(task, data, RequestorButton.RightButton);
        }
    }

    public void CloseButtonPressed()
    {
        // Disable the buttons
        instance.button1.enabled = false;
        instance.button2.enabled = false;

        // Hide the requestor
        instance.requestorBaseObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);

        if (listener != null)
        {
            // Call the response method.
            listener.RequestorResponse(task, data, RequestorButton.CloseButton);
        }
    }
}

public interface RequestorListener
{
    void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result);
}
