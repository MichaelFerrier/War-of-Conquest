using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class CameraMenu : MonoBehaviour
{
    float stateChangeTime = -1;

    void OnEnable()
    {
        stateChangeTime = Time.unscaledTime;
    }

    public void CameraButtonReleased()
    {
        stateChangeTime = Time.unscaledTime;
    }

    void Update()
    {	
        if (((Input.touchCount > 0) || Input.GetMouseButton(0)) && ((Time.unscaledTime - stateChangeTime) > 0.5f) && (GameGUI.instance.cameraButtonPressTime != -1) && (!GameGUI.instance.cameraButtonPressed)) {
            GameGUI.instance.CloseCameraMenu();
        }
    }
}
