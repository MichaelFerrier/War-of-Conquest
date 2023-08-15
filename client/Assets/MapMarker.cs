using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class MapMarker : MonoBehaviour, RequestorListener
{
    public enum Type
    {
        MapView,
        NationArea,
        Flag
    }

    public Sprite iconMapView, iconNationArea, iconFlag;
    public Sprite editButtonMapView, editButtonNationArea;
    public Image iconImage, editButtonImage;
    public TMPro.TextMeshProUGUI flagText;
    public GameObject panel, editButton, textPanel, buttonPair;

    public int blockX, blockZ;
    public Type type;
    public float map_margin;

    RectTransform rectTransform, panelRectTransform;

	public void Init(int _blockX, int _blockZ, MapMarker.Type _type,  string _text, float _map_margin)
    {
        blockX = _blockX;
        blockZ = _blockZ;
        type = _type;
        map_margin = _map_margin;

        rectTransform = gameObject.GetComponent<RectTransform>();
        panelRectTransform = panel.GetComponent<RectTransform>();

        if (type == Type.MapView)
        {
            iconImage.sprite = iconMapView;
            editButtonImage.sprite = editButtonMapView;
            editButton.SetActive(true);
            textPanel.SetActive(false);
            buttonPair.SetActive(false);
        }
        else if (type == Type.NationArea)
        {
            iconImage.sprite = iconNationArea;
            editButtonImage.sprite = editButtonNationArea;
            editButton.SetActive(true);
            textPanel.SetActive(false);
            buttonPair.SetActive(false);
        }
        else if (type == Type.Flag)
        {
            iconImage.sprite = iconFlag;
            editButton.SetActive(false);
            textPanel.SetActive(true);
            buttonPair.SetActive(true);
        }

        flagText.text = _text;
    }

    public void Layout(float _content_ref_width, float _content_width, float _content_height, float _map_rect_width)
    {
        float world_ref_scale = (_content_ref_width - (2 * map_margin)) / MapView.instance.mapDimX;
        float ref_map_scale = (_content_width / _content_ref_width);
        rectTransform.localPosition = new Vector3((map_margin + (blockX * world_ref_scale)) * ref_map_scale, -(map_margin + (blockZ * world_ref_scale)) * ref_map_scale, 0);

        //Debug.Log("Marker block " + blockX + "," + blockZ + ", pos: " + rectTransform.localPosition.x + "," + rectTransform.localPosition.y + ", map_margin: " + map_margin + ", world_ref_scale: " + world_ref_scale + ", ref_map_scale: " + ref_map_scale + ", _content_ref_width: " + _content_ref_width + ", _content_width: " + _content_width);

        float map_rect_ratio = _content_width / _map_rect_width;
        //Debug.Log("_content_width: " + _content_width + ", _map_rect_width: " + _map_rect_width + ", map_rect_ratio: " + map_rect_ratio);

        // Determine whether panel should show at all.
        panel.SetActive(map_rect_ratio >= 2f);

        // Determine width of panel.
        if (panel.activeSelf) 
        {
            panel.GetComponent<RectTransform>().anchoredPosition = new Vector3(0, 20);
            panelRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, (map_rect_ratio - 2f) / 1.5f * 200);
        }
    }

    public void OnClick_Icon()
    {
        // Change the view to the flag's position.
        MapPanel.instance.OnClick_Flag(blockX, blockZ);
    }

    public void OnClick_CreateFlag()
    {
        GameGUI.instance.OpenMapFlagDialog(blockX, blockZ, flagText.text);
    }

    public void OnClick_EditFlag()
    {
        GameGUI.instance.OpenMapFlagDialog(blockX, blockZ, flagText.text);
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
            Network.instance.SendCommand("action=delete_map_flag|x=" + blockX + "|y=" + blockZ);
        }
    }
}
