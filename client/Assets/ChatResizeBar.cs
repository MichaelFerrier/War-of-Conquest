using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;

public class ChatResizeBar : MonoBehaviour, IPointerDownHandler, IDragHandler
{
    public RectTransform chatSystemRectTransform;
    public Chat chatSystem;

    private bool startingShowsChatEnd;
    private Vector2 startingSizeDelta;
    private Vector2 currentPointerPosition;
    private Vector2 previousPointerPosition;

    private float minHeight = 50, maxHeight = 500;
	private bool dragStarted = false;

    // Use this for initialization
    void Start () {
	
	}
	
	// Update is called once per frame
	void Update () {
	
	}

    public void SetHeightLimits(float _minHeight, float _maxHeight)
    {
        minHeight = _minHeight;
        maxHeight = _maxHeight;
    }

    public void OnPointerDown(PointerEventData data)
    {
		// If chat is large, reduce to medium.
		if (Chat.instance.GetDisplayMode() == Chat.ChatDisplayMode.LARGE) 
		{
			Chat.instance.SetDisplayMode(Chat.ChatDisplayMode.MEDIUM);
			dragStarted = false;
			return;
		}

        RectTransformUtility.ScreenPointToLocalPointInRectangle(chatSystemRectTransform, data.position, data.pressEventCamera, out previousPointerPosition);
        startingSizeDelta = chatSystemRectTransform.sizeDelta;
        startingShowsChatEnd = chatSystem.IsShowingChatEnd();
		dragStarted = true;
    }

    public void OnDrag(PointerEventData data)
    {
		if (!dragStarted) {
			return;
		}

        // Determine the new height value for the chat system
        RectTransformUtility.ScreenPointToLocalPointInRectangle(chatSystemRectTransform, data.position, data.pressEventCamera, out currentPointerPosition);
        Vector2 resizeValue = currentPointerPosition - previousPointerPosition;
        Vector2 sizeDelta = startingSizeDelta + new Vector2(0, resizeValue.y);
        sizeDelta = new Vector2(sizeDelta.x, Mathf.Clamp(sizeDelta.y, minHeight, maxHeight));

        // Set the chat system to the new height
        chatSystemRectTransform.sizeDelta = sizeDelta;

        // Alert the chat system of the new chat height
        chatSystem.ChatHeightChanged(sizeDelta.y);

        if (startingShowsChatEnd)
        {
            // Update the canvas with the new layout, before setting the chat log position.
            Canvas.ForceUpdateCanvases();

            // As the size changes, maintain the starting scroll position at the end of chat.
            chatSystem.SetScrollPosition(0f);
        }
    }

}
