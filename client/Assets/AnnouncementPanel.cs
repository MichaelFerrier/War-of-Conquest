using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class AnnouncementPanel : MonoBehaviour
{
    public static AnnouncementPanel instance;
    public TMPro.TextMeshProUGUI announcementText;

    public AnnouncementPanel()
    {
        instance = this;
    }

    public static void Activate(string _announcement)
    {
        // Dispaly the given text.
        instance.announcementText.text = _announcement;

        // Show the announcement panel.
        instance.gameObject.SetActive(true);
        instance.gameObject.GetComponent<GUITransition>().StartTransition(0, 1, 1, 1, false);
    }

    public void OnClick_Okay()
    {
        instance.gameObject.GetComponent<GUITransition>().StartTransition(1, 0, 1, 1, true);
    }
}
