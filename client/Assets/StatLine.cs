using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class StatLine : MonoBehaviour, IPointerClickHandler
{
    public GameData.Stat stat;

    public void Start()
    {
        if ((gameObject.transform.GetSiblingIndex() % 2) == 0)
        {
            gameObject.GetComponent<Image>().sprite = NationPanel.instance.lineDark;
            gameObject.transform.GetChild(0).GetComponent<Image>().color = new Color(0.77f, 0.77f, 0.77f);
        }
        else
        {
            gameObject.GetComponent<Image>().sprite = NationPanel.instance.lineLight;
            gameObject.transform.GetChild(0).GetComponent<Image>().color = new Color(1f, 1f, 1f);
        }
    }

    public void OnPointerClick(PointerEventData _data)
    {
        StatDetailsPanel.instance.Activate(stat);
    }
}
