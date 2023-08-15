using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using I2.Loc;

public class MapFlagLine : MonoBehaviour, IPointerClickHandler, RequestorListener
{
    public int x, z;
    public string title;
    public Text titleText, locationText;

    public void Init(int _x, int _z, string _title)
    {
        x = _x;
        z = _z;
        title = _title;

        titleText.text = title;
        locationText.text = MapView.instance.GetMapLocationText(x, z, true);
    }

    public void InitForSort()
    {
        if ((gameObject.transform.GetSiblingIndex() % 2) == 0)
        {
            gameObject.GetComponent<Image>().sprite = MapPanel.instance.lineDark;
        }
        else
        {
            gameObject.GetComponent<Image>().sprite = MapPanel.instance.lineLight;
        }
    }

    public void OnPointerClick(PointerEventData _data)
    {
        // Change the view to the flag's position.
        MapPanel.instance.OnClick_Flag(x, z);
    }

    public void OnClick_EditFlag()
    {
        GameGUI.instance.OpenMapFlagDialog(x, z, title);
    }

    public void OnClick_DeleteFlag()
    {
        Requestor.Activate(0, 0, this, LocalizationManager.GetTranslation("map_flag_delete"), LocalizationManager.GetTranslation("Generic Text/yes_word"), LocalizationManager.GetTranslation("Generic Text/no_word"));
    }

    public void RequestorResponse(int _task, int _data, Requestor.RequestorButton _result)
    {
        if (_result == Requestor.RequestorButton.LeftButton)
        {
            // Delete this map flag.
            Network.instance.SendCommand("action=delete_map_flag|x=" + x + "|y=" + z);
        }
    }
}
