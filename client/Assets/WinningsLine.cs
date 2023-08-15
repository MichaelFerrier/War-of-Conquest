using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System.Collections;
using I2.Loc;

public class WinningsLine : MonoBehaviour, IPointerClickHandler, IPointerDownHandler, IPointerUpHandler
{
    public TMPro.TextMeshProUGUI nameText, nameOutlineText, amountText, amountOutlineText;
    public Image backgroundImage;
    public Sprite oddBG, evenBG;
    public int x, z;

    public void Init(NationOrbRecord _nation_orb_record, int _index)
    {
        backgroundImage.sprite = ((_index % 2) == 1) ? oddBG : evenBG;

        ObjectData objectData = ObjectData.GetObjectData(_nation_orb_record.objectID);

        // Record coords
        x = _nation_orb_record.x;
        z = _nation_orb_record.z;

        if (_nation_orb_record.currentlyOccupied)
        {
            nameText.gameObject.SetActive(false);
            nameOutlineText.gameObject.SetActive(true);
            amountText.gameObject.SetActive(false);
            amountOutlineText.gameObject.SetActive(true);

            nameOutlineText.text = ((objectData == null) ? LocalizationManager.GetTranslation("Connect Panel/invalid_orb") : objectData.name) + " (" + MapView.instance.GetMapLocationText(x, z, true) + ")";
            amountOutlineText.text = "$" + (_nation_orb_record.winnings / 100.0).ToString("N2")/*string.Format("{0:n0}", _nation_orb_record.winnings) + " <sprite=2>"*/;
        }
        else
        {
            nameText.gameObject.SetActive(true);
            nameOutlineText.gameObject.SetActive(false);
            amountText.gameObject.SetActive(true);
            amountOutlineText.gameObject.SetActive(false);

            nameText.text = ((objectData == null) ? LocalizationManager.GetTranslation("Connect Panel/invalid_orb") : objectData.name) + " (" + MapView.instance.GetMapLocationText(x, z, true) + ")";
            amountText.text = "$" + (_nation_orb_record.winnings / 100.0).ToString("N2")/*string.Format("{0:n0}", _nation_orb_record.winnings) + " <sprite=2>"*/;
        }
    }

    public void OnPointerClick(PointerEventData _data)
    {
        // If we're not looking at the mainland map, do nothing.
        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        // Send message to server, to set view to the map location of this orb.
        Network.instance.SendCommand("action=event_center_on_block|blockX=" + x + "|blockY=" + z, true);
    }

    public void OnPointerUp(PointerEventData _data)
    {
        // For some reason OnPointerUp() and OnPointerDown() need to be implemented in order for OnPointerClick() to work.
    }

    public void OnPointerDown(PointerEventData _data)
    {
        // For some reason OnPointerUp() and OnPointerDown() need to be implemented in order for OnPointerClick() to work.
    }
}