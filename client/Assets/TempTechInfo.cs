using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using I2.Loc;
using UnityEngine.EventSystems;

public class TempTechInfo : MonoBehaviour
{
    public static TempTechInfo instance;

    public RectTransform tempTechInfoRect;
    public TMPro.TextMeshProUGUI text;

    TempIcon tempIcon;
    float prev_update_time = -1, stateChangeTime = -1;
    Vector3[] rectCorners = new Vector3[4];

    public TempTechInfo()
    {
        instance = this;
    }

    public static void Activate(TempIcon _icon)
    {
        instance.tempIcon = _icon;
        instance.gameObject.SetActive(true);
        instance.UpdatePositionAndText();
        instance.gameObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
        instance.stateChangeTime = Time.unscaledTime;
    }

    public static void Deactivate()
    {
        instance.gameObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
        instance.stateChangeTime = Time.unscaledTime;
    }
	
	void Update()
    {	
        if (((Input.touchCount > 0) || Input.GetMouseButton(0)) && ((Time.unscaledTime - stateChangeTime) > 0.5f)) {
            Deactivate();
        }

        if ((Time.unscaledTime - prev_update_time) >= .5f)
        {
            UpdatePositionAndText();
            prev_update_time = Time.unscaledTime;
        }
	}

    void UpdatePositionAndText()
    {
        // If the icon that spawned this has been deleted, close this.
        if (tempIcon == null) 
        {
            TempTechInfo.Deactivate();
            return;
        }

        // Update text
        text.text = "<sprite=3>  " + tempIcon.techData.name + ((tempIcon.expireTime <= Time.unscaledTime) ? "" : ("   (" + GameData.instance.ConstructTimeRemainingString(tempIcon.techID) + " " + LocalizationManager.GetTranslation("Generic Text/remaining_text") + ")"));

        // Update position
        tempIcon.gameObject.GetComponent<RectTransform>().GetWorldCorners(rectCorners);
        //Debug.Log("Corners x0: " + rectCorners[1].x + ", x1: " + rectCorners[3].x + ", y0: " + rectCorners[3].y + ", y1: " + rectCorners[1].y);
        float targetX = (rectCorners[1].x + ((rectCorners[3].x - rectCorners[1].x) / 2));
        float targetY = (rectCorners[3].y + ((rectCorners[1].y - rectCorners[3].y) / 2));
        //Debug.Log("scaleFactor: " + MapView.instance.canvas.scaleFactor + ", targetX: " + targetX + ", targetY: " + targetY);
        tempTechInfoRect.anchoredPosition = new Vector3(targetX / MapView.instance.canvas.scaleFactor, targetY / MapView.instance.canvas.scaleFactor, 0);
    }

    public void OnPress()
    {
        // Open the advance details panel to display info on this advance.
        AdvanceDetailsPanel.instance.Activate(tempIcon.techID);

        //// Display the Advances Panel with info about this temp tech
        //GameGUI.instance.SetActiveGamePanel(GameGUI.GamePanel.GAME_PANEL_ADVANCES);
        //AdvancesPanel.instance.SelectAdvance(tempIcon.techID, true);

        // Close the temp tech info popup
        TempTechInfo.Deactivate();
    }
}
