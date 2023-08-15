using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class ZoomRect : MonoBehaviour
{
    public enum PanningType
    {
        NONE,
        MOUSE,
        ONE_TOUCH,
        TWO_TOUCH
    };

    public enum ZoomMode
    {
        Resize,
        Scale
    };

    public delegate void ResizeHandler();
    public event ResizeHandler OnResize;

    public const float KEYBOARD_ZOOM_SPEED = 1.5f;
    public const float SCROLL_WHEEL_ZOOM_SPEED = 0.1f;

    public RectTransform content;
    public RectTransform zoomRectTransform, viewportRectTransform;
    public Scrollbar horizontalScrollbar, verticalScrollbar;
    public ZoomMode zoomMode;

    private float PAN_CONTINUE_PERIOD = 0.5f;
    private float VIEW_MAX_OUT_OF_BOUNDS = 150f;
    private float CAMERA_MAX_PAN_SPEED = 1000f;

    Vector2 panStartPoint, targetPoint, prevTargetPoint, prevPanRate;
    Vector3 pressPosition;
    float pressTime, startTouchDistance, startContentScale, prevPanTime;
    PanningType panning;
    Vector2 contentRefSize = new Vector2(1000,1000), outOfBoundsAmount = new Vector2(0, 0), outOfBoundsCompensation = new Vector2(0, 0);
    float minContentScale = 0.3f; // Have this be changed dynamically
    float maxContentScale = 1.5f;
    float hOuterMargin, vOuterMargin;
    bool viewOutOfBounds = false;
    bool updatingScrollbars = false;

    public void Start()
    {
        if (horizontalScrollbar) {
            horizontalScrollbar.onValueChanged.AddListener(SetHorizontalNormalizedPosition);
        }

        if (verticalScrollbar) {
            verticalScrollbar.onValueChanged.AddListener(SetVerticalNormalizedPosition);
        }
    }

    private void OnEnable()
    {
        DetermineMargins();
    }

    // Update is called once per frame
    void Update ()
    {
        bool panView = false;

        if (GameGUI.instance.IsPanelOpen() == false)
        {
		    if (Input.GetMouseButtonDown(0) && IsPointDirectlyOnContent(Input.mousePosition)) 
            {
                // Mouse button 0 has just been pressed down, over the zoom rect's content. Begin panning.    
			    panStartPoint = prevTargetPoint = GetNormalizedContentPoint(Input.mousePosition);
			    pressPosition = Input.mousePosition;
			    pressTime = Time.unscaledTime;
			    panning = PanningType.MOUSE;
                //Debug.Log("UI ZoomRect normalized press at " + panStartPoint + ", screen pos: " + pressPosition);
		    }
		    if (Input.GetMouseButton(0) && (panning == PanningType.MOUSE)) 
            {
                // Mouse button 0 continues to be pressed down, and panning has already begun. Pan the view given the new target point.
			    targetPoint = this.GetNormalizedContentPoint(Input.mousePosition);
			    panView = true;
		    }
		    if (Input.GetMouseButtonUp(0)  && IsPointDirectlyOnContent(Input.mousePosition) && (Vector3.Distance(pressPosition, Input.mousePosition) < (Screen.width / 100.0f)) && ((Time.unscaledTime - pressTime) < 0.5f)) 
            {
                // Mouse button 0 has been tapped, without dragging. Count this as a click.
			    targetPoint = this.GetNormalizedContentPoint(Input.mousePosition);
		    }

            // Handle one-touch panning
		    if (Input.touchCount == 1)
		    {
			    Touch currentTouch = Input.GetTouch(0);

			    if ((panning == PanningType.TWO_TOUCH) || ((currentTouch.phase == TouchPhase.Began) && IsPointDirectlyOnContent(currentTouch.position))) 
                {
				    panStartPoint = GetNormalizedContentPoint(currentTouch.position);
                    pressTime = Time.unscaledTime;
                    pressPosition = currentTouch.position;
				    panning = PanningType.ONE_TOUCH;
			    }
			    else if (((currentTouch.phase == TouchPhase.Moved) || (currentTouch.phase == TouchPhase.Stationary)) && (panning == PanningType.ONE_TOUCH)) 
                {
				    targetPoint = GetNormalizedContentPoint(Input.mousePosition);
				    panView = true;
			    }
		    }

            // Handle two-touch panning and zooming
		    if (Input.touchCount == 2)
		    {
			    Touch currentTouch0 = Input.GetTouch(0);
                Touch currentTouch1 = Input.GetTouch(1);
			
			    if ((panning == PanningType.ONE_TOUCH) || ((currentTouch0.phase == TouchPhase.Began) && IsPointDirectlyOnContent(currentTouch0.position)) || ((currentTouch1.phase == TouchPhase.Began) && IsPointDirectlyOnContent(currentTouch1.position))) 
                {
                    // Record starting values. Use the point between the world points of the two touches as the starting point.
                    panStartPoint = Vector3.Lerp(GetNormalizedContentPoint(currentTouch0.position), GetNormalizedContentPoint(currentTouch1.position), 0.5f);
                    startContentScale = (zoomMode == ZoomMode.Resize) ? (content.sizeDelta.x / contentRefSize.x) : content.localScale.x;
                    startTouchDistance = Vector2.Distance(currentTouch0.position, currentTouch1.position);
				    panning = PanningType.TWO_TOUCH;
			    }
			    else if (((currentTouch0.phase == TouchPhase.Moved) || (currentTouch0.phase == TouchPhase.Stationary) || (currentTouch1.phase == TouchPhase.Moved) || (currentTouch1.phase == TouchPhase.Stationary)) && (panning == PanningType.TWO_TOUCH)) 
                {
                    // Use the point between the normalized points of the two touches as the target point for the pan.
				    targetPoint = Vector3.Lerp(GetNormalizedContentPoint(currentTouch0.position), GetNormalizedContentPoint(currentTouch1.position), 0.5f);
				    panView = true;

                    // Determine the new content size, based on the ratio of the current distance between the two touches, to the starting distance.
                    float curTouchDistance = Vector2.Distance(currentTouch0.position, currentTouch1.position);
                    float new_content_scale = Mathf.Min(maxContentScale, Mathf.Max(minContentScale, (curTouchDistance / startTouchDistance) * startContentScale));
                    ResizeContentAroundNormalizedPoint(new Vector2(contentRefSize.x * new_content_scale, contentRefSize.y * new_content_scale), panStartPoint);
			    }
		    }

            float zoomDelta = (Input.mouseScrollDelta.y * SCROLL_WHEEL_ZOOM_SPEED);
		    if (Input.GetKey ("up")) zoomDelta = KEYBOARD_ZOOM_SPEED * Time.unscaledDeltaTime;
		    if (Input.GetKey ("down")) zoomDelta = -KEYBOARD_ZOOM_SPEED * Time.unscaledDeltaTime;
		    if (zoomDelta != 0f)
		    {
                float curContentScale = (zoomMode == ZoomMode.Resize) ? (content.sizeDelta.x / contentRefSize.x) : content.localScale.x;
                zoomDelta *= curContentScale;
                float new_content_scale = Mathf.Min(maxContentScale, Mathf.Max(minContentScale, curContentScale + zoomDelta));
                ResizeContentAroundRectCenter(new Vector2(contentRefSize.x * new_content_scale, contentRefSize.y * new_content_scale));
		    }
		}

        if ((panning != PanningType.NONE) && (Input.touchCount == 0) && (Input.GetMouseButton(0) == false)) {
			panning = PanningType.NONE;
		}

		if (panView)
		{
			// Only move the view if the target position has changed by greater than a certain small amount.
			float xDif = Mathf.Abs(targetPoint.x - prevTargetPoint.x);
			float yDif = Mathf.Abs(targetPoint.y - prevTargetPoint.y);
            if ((xDif > 0.001f) || (yDif > 0.001f))
			{
                // Determine vector describing how pan target point has moved away from its starting position.
   				Vector2 panDelta = targetPoint - panStartPoint - outOfBoundsCompensation;

                // Move the content for this pan.
                content.localPosition = new Vector3(content.localPosition.x + (panDelta.x * (content.sizeDelta.x * content.localScale.x)), content.localPosition.y - (panDelta.y * (content.sizeDelta.y * content.localScale.y)));

                UpdateScrollbars();

                // Determine whether the content has been panned out of the allowed area.
                CheckViewAgainstBounds();

                // If the pan is greater than a small amount...
                if ((xDif > 0.001f) || (yDif > 0.001f))
			    {
                    // Record rate (normalized content movement per second) of this latest pan, so the pan will be continued after the player lets go.  
                    prevPanRate = panDelta * (1 / Time.unscaledDeltaTime);
                    prevPanTime = Time.unscaledTime;
                }
                else 
                {
                    // The pan is for too small an amount to be accurately continued.
                    prevPanTime = 0f;
                }
            }
		}
        else if ((!viewOutOfBounds) && (prevPanTime != 0))
        {
            // Determine the degree to which the previous pan should be continued now. It will trail off over PAN_CONTINUE_PERIOD.
            float degree = Mathf.Max(0, PAN_CONTINUE_PERIOD - (Time.unscaledTime - prevPanTime));
            
            if (degree == 0)
            {
                // The pan continue period since the last pan is over. Stop continuing the pan. 
                prevPanTime = 0;
            }
            else 
            {
                // Move the content to continue the latest pan.

                // Determine vector describing how pan target point has moved away from its starting position.
   				Vector2 panDelta = prevPanRate * degree * Time.unscaledDeltaTime;

                // Move the content for this pan.
                content.localPosition = new Vector3(content.localPosition.x + (panDelta.x * (content.sizeDelta.x * content.localScale.x)), content.localPosition.y - (panDelta.y * (content.sizeDelta.y * content.localScale.y)));

                UpdateScrollbars();

                // Determine whether the content has been panned out of the allowed area.
                CheckViewAgainstBounds();
            }
        }

        if (viewOutOfBounds && (panning == PanningType.NONE))
        {
            // Determine the degree to which the camera position will be moved back into the allowed area.
            // A small constant amount is added so that it doesn't stop moving until it is firmly back within the allowed area.
            float amount_return_x = Mathf.Max(-1f, Mathf.Min(1f, (-outOfBoundsAmount.x) / VIEW_MAX_OUT_OF_BOUNDS));
            float amount_return_y = Mathf.Max(-1f, Mathf.Min(1f, (-outOfBoundsAmount.y) / VIEW_MAX_OUT_OF_BOUNDS));

            if ((amount_return_x != 0f) || (amount_return_y != 0f))
            {
                // Move the contents back in the direction of the allowed view area.
                Vector2 panDelta = new Vector2(amount_return_x * CAMERA_MAX_PAN_SPEED * Time.deltaTime, amount_return_y * CAMERA_MAX_PAN_SPEED * Time.unscaledDeltaTime);

                // Add a small amount to the paneDelta, to make sure the content returns solidly within bounds, rather than infinitely getting incrementally closer.
                if (panDelta.x != 0) panDelta.x += Mathf.Sign(panDelta.x) * 0.1f;
                if (panDelta.y != 0) panDelta.y += Mathf.Sign(panDelta.y) * 0.1f;

                // Change the position of the content to bring it closer to being back in bounds.
                content.localPosition = new Vector3(content.localPosition.x + panDelta.x, content.localPosition.y + panDelta.y);

                UpdateScrollbars();
            }

            CheckViewAgainstBounds();
            //Debug.Log("viewOutOfBounds: " + viewOutOfBounds + ", outOfBoundsAmount.x: " + outOfBoundsAmount.x + ", outOfBoundsAmount.y: " + outOfBoundsAmount.y);
        }
	}

    public void ContentPositionChanged()
    {
        UpdateScrollbars();
        CheckViewAgainstBounds();
    }

    public void SetContentRefSize(Vector2 _contentRefSize)
    {
        contentRefSize = _contentRefSize;
        DetermineMinScale();
        DetermineMargins();
    }

    public void DetermineMargins()
    {
        hOuterMargin = zoomRectTransform.sizeDelta.x / 2;
        vOuterMargin = zoomRectTransform.sizeDelta.y / 2;
        UpdateScrollbars();
    }

    public Vector2 GetContentRefSize()
    {
        return contentRefSize;
    }

    public void DetermineMinScale()
    {
        // Determine the new minContentScale, so that there will be no empty space on any side of the content. -1 to make sure the min scale is at least large enough to keep the whole image in bounds.
        minContentScale = Mathf.Max(zoomRectTransform.rect.width / (contentRefSize.x - 1), zoomRectTransform.rect.height / (contentRefSize.y - 1));

        // If the content is currently smaller than the new min scale, resize it to the new min scale.
        float curContentScale = (zoomMode == ZoomMode.Resize) ? (content.sizeDelta.x / contentRefSize.x) : content.localScale.x;
        if (curContentScale < minContentScale) {
            ResizeContentAroundRectCenter(new Vector2(contentRefSize.x * minContentScale, contentRefSize.y * minContentScale));
        }
    }

    public void CheckViewAgainstBounds()
    {
        viewOutOfBounds = ((content.localPosition.x <= -((content.sizeDelta.x * content.localScale.x) + hOuterMargin - zoomRectTransform.sizeDelta.x)) || (content.localPosition.x > hOuterMargin) || (content.localPosition.y >= ((content.sizeDelta.y * content.localScale.y) + vOuterMargin - zoomRectTransform.sizeDelta.y)) || (content.localPosition.y < -vOuterMargin));
        if (viewOutOfBounds)
        {
            if (content.localPosition.x <= (-((content.sizeDelta.x * content.localScale.x)  + hOuterMargin - zoomRectTransform.sizeDelta.x))) outOfBoundsAmount.x = content.localPosition.x + ((content.sizeDelta.x * content.localScale.x) + hOuterMargin - zoomRectTransform.sizeDelta.x);
            else if (content.localPosition.x > hOuterMargin) outOfBoundsAmount.x = content.localPosition.x - hOuterMargin;
            else outOfBoundsAmount.x = 0;

            if (content.localPosition.y < -vOuterMargin) outOfBoundsAmount.y = content.localPosition.y + vOuterMargin;
            else if (content.localPosition.y > ((content.sizeDelta.y * content.localScale.y) + vOuterMargin - zoomRectTransform.sizeDelta.y)) outOfBoundsAmount.y = content.localPosition.y - ((content.sizeDelta.y * content.localScale.y) + vOuterMargin - zoomRectTransform.sizeDelta.y);
            else outOfBoundsAmount.y = 0;

            // Dampen transition to new outOfBoundsCompensation value, to avoid bouncing.
            outOfBoundsCompensation.x = (outOfBoundsCompensation.x * 0.8f) + (RubberDelta(outOfBoundsAmount.x) / (content.sizeDelta.x * content.localScale.x) * 0.2f);
            outOfBoundsCompensation.y = (outOfBoundsCompensation.y * 0.8f) + (-(RubberDelta(outOfBoundsAmount.y) / (content.sizeDelta.y * content.localScale.y)) * 0.2f);
        }
        else
        {
            outOfBoundsAmount.x = 0;
            outOfBoundsAmount.y = 0;
            outOfBoundsCompensation.x = 0;
            outOfBoundsCompensation.y = 0;
        }
    }

    private float RubberDelta(float _overStretching)
    {
        float allowedOverStretch = (1 / ((Mathf.Abs(_overStretching) * 50.0f / zoomRectTransform.sizeDelta.x) + 1)) * _overStretching;
        float compensationDistance = _overStretching - allowedOverStretch;
        //Debug.Log("_overStretching: " + _overStretching + ", allowedOverStretch: " + allowedOverStretch + ", compensationDistance: " + compensationDistance);
        return compensationDistance;
        //return (1 - (1 / ((Mathf.Abs(_overStretching) * 0.55f / zoomRectTransform.sizeDelta.x) + 1))) * zoomRectTransform.sizeDelta.x * Mathf.Sign(_overStretching);
    }

    public void ResizeContentAroundRectCenter(Vector2 _size)
    {
        Rect screen_rect = GameGUI.RectTransformToScreenSpace(this.GetComponent<RectTransform>());
        ResizeContentAroundNormalizedPoint(_size, new Vector2(((zoomRectTransform.rect.width / 2) + (-content.localPosition.x)) / (content.sizeDelta.x * content.localScale.x), ((zoomRectTransform.rect.height / 2) + content.localPosition.y) / (content.sizeDelta.y * content.localScale.y)));
    }

    public void ResizeContentAroundNormalizedPoint(Vector2 _size, Vector2 _anchor)
    {
        float prevX = content.localPosition.x;
        float prevY = content.localPosition.y;
        float deltaX = ((content.sizeDelta.x * content.localScale.x) - _size.x) * _anchor.x;
        float deltaY = (_size.y - (content.sizeDelta.y * content.localScale.y)) * _anchor.y;

        if (zoomMode == ZoomMode.Resize)
        {
            content.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, _size.x);
            content.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, _size.y);

            // Update content layout for resize.
            if (OnResize != null) {
                 OnResize();
            }
        }
        else if (zoomMode == ZoomMode.Scale)
        {
            content.localScale = new Vector3(_size.x / contentRefSize.x, _size.y / contentRefSize.y, 1);
        }

        // Change the position (must be done after changing size, which messes up the position).
        content.localPosition = new Vector3(prevX + deltaX, prevY + deltaY, 0);
        
        UpdateScrollbars();

        // Check whether the content is now out of bounds at its new size.
        CheckViewAgainstBounds();
    }

    public Vector2 GetNormalizedContentPoint(Vector3 _screen_point)
    {
        Rect screen_rect = GameGUI.RectTransformToScreenSpace(this.GetComponent<RectTransform>());
        return new Vector2((((_screen_point.x - screen_rect.x) / MapView.instance.canvas.scaleFactor) + (-content.localPosition.x)) / (content.sizeDelta.x * content.localScale.x), (((screen_rect.height - (_screen_point.y - screen_rect.y)) / MapView.instance.canvas.scaleFactor) + content.localPosition.y) / (content.sizeDelta.y * content.localScale.y));
    }

    public bool IsPointDirectlyOnContent(Vector2 _point)
    {
        return RectTransformUtility.RectangleContainsScreenPoint(viewportRectTransform, _point);
        /*
        //GraphicRaycaster gr = this.GetComponent<GraphicRaycaster>();
        GraphicRaycaster gr = MapView.instance.canvas.gameObject.GetComponent<GraphicRaycaster>();
        
        //Create the PointerEventData with null for the EventSystem
        PointerEventData ped = new PointerEventData(null);
        ped.position = _point;

        //Create list to receive all results
        List<RaycastResult> results = new List<RaycastResult>();
        
        //Raycast it
        gr.Raycast(ped, results);

        foreach (RaycastResult rr in results)
        {
            if (rr.gameObject == content.gameObject) {
                //Debug.Log("Depth: " + rr.depth); // 101
                return true;
            }
        }

        return false;
        */
    }

    public void UpdateScrollbars()
    {
        updatingScrollbars = true;

        if (horizontalScrollbar != null)
        {
            float range = (content.sizeDelta.x * content.localScale.x) + (2 * hOuterMargin) - zoomRectTransform.sizeDelta.x;
            float outOfBoundsAmount = (content.localPosition.x < -(range - hOuterMargin)) ? (-(range - hOuterMargin) - content.localPosition.x) : ((content.localPosition.x > hOuterMargin) ? (content.localPosition.x - hOuterMargin) : 0); 
            float pos = (content.localPosition.x - hOuterMargin) / -range;
            horizontalScrollbar.size = Mathf.Max(1, zoomRectTransform.sizeDelta.x - outOfBoundsAmount) / (content.sizeDelta.x * content.localScale.x + (2 * hOuterMargin));
            horizontalScrollbar.value = Mathf.Max(0, Mathf.Min(1, pos));
        }

        if (verticalScrollbar != null)
        {
            float range = (content.sizeDelta.y * content.localScale.y) + (2 * vOuterMargin) - zoomRectTransform.sizeDelta.y;
            float outOfBoundsAmount = (content.localPosition.y > (range - vOuterMargin)) ? (content.localPosition.y - (range - vOuterMargin)) : ((content.localPosition.y < -vOuterMargin) ? (-vOuterMargin - content.localPosition.y) : 0); 
            float pos = 1f - ((content.localPosition.y + vOuterMargin) / range);
            verticalScrollbar.size = Mathf.Max(1, zoomRectTransform.sizeDelta.y - outOfBoundsAmount) / (content.sizeDelta.y * content.localScale.y + (2 * vOuterMargin));
            verticalScrollbar.value = Mathf.Max(0, Mathf.Min(1, pos));
        }

        updatingScrollbars = false;
    }

    public void SetHorizontalNormalizedPosition(float value)
    {
        if (updatingScrollbars) return;
        float range = (content.sizeDelta.x * content.localScale.x) + (2 * hOuterMargin) - zoomRectTransform.sizeDelta.x;
        float pos = value * -range + hOuterMargin;
        content.localPosition = new Vector3(pos, content.localPosition.y);
    }

    public void SetVerticalNormalizedPosition(float value)
    {
        if (updatingScrollbars) return;
        float range = (content.sizeDelta.y * content.localScale.y) + (2 * vOuterMargin) - zoomRectTransform.sizeDelta.y;
        float pos = (1f - value) * range - vOuterMargin;
        content.localPosition = new Vector3(content.localPosition.x, pos);
    }
}
