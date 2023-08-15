using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ForceLandscapeWhenActive : MonoBehaviour
{
    public static int forceLandscapeCount = 0;

    private void OnEnable()
    {
        forceLandscapeCount++;

        if ((forceLandscapeCount > 0) && (GameGUI.instance != null)) {
            GameGUI.instance.ForceLandscape(true);
        }
    }

    private void OnDisable()
    {
        forceLandscapeCount--;

        if ((forceLandscapeCount <= 0) && (GameGUI.instance != null)) {
            GameGUI.instance.ForceLandscape(false);
        }
    }
}
