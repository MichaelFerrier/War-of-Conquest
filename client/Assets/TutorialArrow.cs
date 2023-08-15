using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class TutorialArrow : MonoBehaviour
{
    enum Mode
    {
        UI,
        BLOCK,
        CENTER
    }

    const float FADE_DURATION = 0.4f;
    const float FULL_ALPHA = 0.7f;

    public RectTransform arrowImageRectTransform, compassRoseRectTransform;
    public Image arrowImage;
    int blockX = -1, blockZ = -1;
    Mode mode;
    RectTransform rectUI;

    float startAlpha, endAlpha, fadeStartTime = -1;
    bool deactivateAfterFade = false;

    Vector3[] rectCorners = new Vector3[4];

	void Update ()
    {
		if ((mode == Mode.BLOCK) || (mode == Mode.CENTER)) {
            UpdatePositionOnMap();
        }

        // Bounce the arrow
        if (mode == Mode.CENTER) {
            arrowImageRectTransform.localPosition = new Vector3(0f, (1f - Mathf.Abs(Mathf.Sin(Time.unscaledTime))) * 40f, 0f);
        } else {
            arrowImageRectTransform.localPosition = new Vector3(0f, Mathf.Abs(Mathf.Sin(Time.unscaledTime)) * 20f, 0f);
        }

        // Update fade if necessary
        if (fadeStartTime != -1)
        {
            if (Time.unscaledTime >= (fadeStartTime + FADE_DURATION))
            {
                fadeStartTime = -1;

                arrowImage.color = new Color(1, 1, 1, endAlpha);

                if (deactivateAfterFade) {
                    gameObject.SetActive(false);
                }
            }
            else
            {
                float progress = (Time.unscaledTime - fadeStartTime) / FADE_DURATION;
                arrowImage.color = new Color(1, 1, 1, (endAlpha - startAlpha) * progress + startAlpha);
            }
        }
	}

    public void ActivateForUI(RectTransform _rectUI)
    {
        // Set mode
        mode = Mode.UI;

        // Reset values
        blockX = blockZ = -1;

        // Record UI rect transform
        rectUI = _rectUI;

        // Hide the compass rose
        compassRoseRectTransform.gameObject.SetActive(false);

        // Activate the arrow
        gameObject.SetActive(true);

        // Parent it to the canvas
        transform.SetParent(GameGUI.instance.canvasRectTransform);
        transform.SetAsLastSibling();
        transform.localScale = new Vector3(1,1,1);
        arrowImageRectTransform.pivot = new Vector2(0.5f, 0f);
        transform.localPosition = new Vector3(0, 0, 0);

        // Update the arrow's position on the UI
        UpdatePositionOnUI();

        // Begin fade in
        StartFade(0f, FULL_ALPHA, false);
    }

    public void ActivateForBlock(int _blockX, int _blockZ)
    {
        if ((_blockX == -1) || (_blockZ == -1)) {
            return;
        }

        // Set mode
        mode = Mode.BLOCK;

        // Reset values
        rectUI = null;

        // Record the block coordinates
        blockX = _blockX;
        blockZ = _blockZ;

        // Hide the compass rose
        compassRoseRectTransform.gameObject.SetActive(false);

        // Activate the arrow
        gameObject.SetActive(true);
        
        // Parent it to the map's overlay panel
        transform.SetParent(MapView.instance.overlay_panel_1_rect_transform);
        transform.SetAsLastSibling();
        transform.localScale = new Vector3(1,1,1);
        arrowImageRectTransform.pivot = new Vector2(0.5f, 0f);

        // Reset the arrow's rotation
        GetComponent<RectTransform>().eulerAngles = new Vector3(0, 0, 0);
        
        // Update the arrow's position on the map
        UpdatePositionOnMap();

        // Begin fade in
        StartFade(0f, FULL_ALPHA, false);
    }

    public void ActivateToShowEast()
    {
        // Set mode
        mode = Mode.CENTER;

        // Reset values
        rectUI = null;
        blockX = -1;
        blockZ = -1;

        // Show the compass rose
        compassRoseRectTransform.gameObject.SetActive(true);

        // Activate the arrow
        gameObject.SetActive(true);
        
        // Parent it to the map's overlay panel
        transform.SetParent(GameGUI.instance.canvasRectTransform);
        transform.SetAsLastSibling();
        transform.localScale = new Vector3(1,1,1);
        arrowImageRectTransform.pivot = new Vector2(0.5f, 2.5f);
        transform.localPosition = new Vector3(0, 0, 0);

        // Set the arrow's rotation
        GetComponent<RectTransform>().eulerAngles = new Vector3(45, 0, 135);
        
        // Update the arrow's position on the map
        UpdatePositionOnMap();

        // Begin fade in
        StartFade(0f, FULL_ALPHA, false);
    }

    public void Deactivate()
    {
        // Hide the compass rose
        compassRoseRectTransform.gameObject.SetActive(false);

        // Fade out the arrow, and deactivate.
        StartFade(FULL_ALPHA, 0f, true);

        blockX = blockZ = -1;
    }

    public void StartFade(float _startAlpha, float _endAlpha, bool _deactivateAfterFade)
    {
        if (fadeStartTime == -1) {
            startAlpha = _startAlpha;
        } else {
            startAlpha = arrowImage.color.a;
        }

        endAlpha = _endAlpha;
        deactivateAfterFade = _deactivateAfterFade;
        fadeStartTime = Time.unscaledTime;
    }

    public void UpdatePositionOnMap()
    {
        if (mode == Mode.BLOCK) 
        {
            transform.position = MapView.instance.GetBlockCenterScreenPos(blockX, blockZ);
        }
        else if (mode == Mode.CENTER)
        {
            GetComponent<RectTransform>().anchoredPosition = new Vector3(Screen.width / 2 / MapView.instance.canvas.scaleFactor, Screen.height / 2 / MapView.instance.canvas.scaleFactor, 0);
        }
    }

    public void UpdatePositionOnUI()
    {
        rectUI.GetWorldCorners(rectCorners);
        //Debug.Log("Corners x0: " + rectCorners[1].x + ", x1: " + rectCorners[3].x + ", y0: " + rectCorners[3].y + ", y1: " + rectCorners[1].y);
        float targetX = (rectCorners[1].x + ((rectCorners[3].x - rectCorners[1].x) / 2));
        float targetY = (rectCorners[3].y + ((rectCorners[1].y - rectCorners[3].y) / 2));
        //Debug.Log("scaleFactor: " + MapView.instance.canvas.scaleFactor + ", targetX: " + targetX + ", targetY: " + targetY);
        GetComponent<RectTransform>().anchoredPosition = new Vector3(targetX / MapView.instance.canvas.scaleFactor, targetY / MapView.instance.canvas.scaleFactor, 0);
        Vector2 angleVector0 = new Vector2(Screen.width / 2, Screen.height / 2);
        Vector2 angleVector1 = new Vector2(targetX, targetY);
        float angle = Vector2.Angle(Vector2.up, angleVector1 - angleVector0);
        if (angleVector1.x < angleVector0.x) {
            angle = -angle;
        }
        //Debug.Log("Screen width: " + Screen.width + ", height: " + Screen.height);
        //Debug.Log("angleVector0: " + angleVector0 + ", angleVector1: " + angleVector1 + ", Angle: " + angle);
        GetComponent<RectTransform>().eulerAngles = new Vector3(0, 0, 180f - angle);
    }
}
