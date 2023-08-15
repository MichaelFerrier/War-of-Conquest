using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using UnityEngine.EventSystems;

public class Alert : MonoBehaviour, IPointerDownHandler
{
    public static Alert instance;

    public GameObject alertBaseObject, alertContainerObject;
    public RectTransform alertRectTransform;
    public TMPro.TextMeshProUGUI text;
    public GUITransition alertTransition;

    public Alert()
    {
        instance = this;
    }

    public static void Activate(string _alert_text)
    {
        // Set the alert's text.
        instance.text.text = _alert_text;

        // Show the alert.
        instance.alertBaseObject.SetActive(true);

        instance.alertTransition.StartTransition(0,1,0,1,false);
    }

    public static void Deactivate()
    {
        instance.alertTransition.StartTransition(1,0,1,0,true);
    }

    public void SetActiveFalse()
    {
        instance.alertBaseObject.SetActive(false);
    }

    public void OnPointerDown(PointerEventData eventData)
    {
        Alert.Deactivate();

        // Handle event here AND in ancestors.
        // This allows event to bubble up and be handled by the AdvancesPanel, also. See https://coeurdecode.com/2015/10/20/bubbling-events-in-unity/
        ExecuteEvents.ExecuteHierarchy(transform.parent.gameObject, eventData, ExecuteEvents.pointerDownHandler);
    }
}