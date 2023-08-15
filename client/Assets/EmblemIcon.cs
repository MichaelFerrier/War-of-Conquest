using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class EmblemIcon : MonoBehaviour, IPointerDownHandler, IPointerClickHandler
{
    public Sprite emblemNoneSprite;
    public GameObject selection_overlay;
    public int index;

    public void Init(int _index, Texture2D _tex, int _x, int _y, int _dim)
    {
        // Record the index
        index = _index;

        if (index == -1)
        {
            // Use sprite representing no emblem.
            gameObject.GetComponent<Image>().sprite = emblemNoneSprite;
        }
        else
        {
            // Create the sprite
            Texture2D tex2 = new Texture2D(_dim,_dim);
            Color[] colors = _tex.GetPixels(_x,_tex.height - _y - 1 - _dim,_dim,_dim);
            tex2.SetPixels(colors);
            tex2.Apply();
            Sprite sprite = Sprite.Create(tex2, new Rect(0,0,_dim,_dim),new Vector2(0.0f,0.0f),1.0f);
            gameObject.GetComponent<Image>().sprite = sprite;
        }

        // By default turn off the selection overlay.
        selection_overlay.SetActive(false);
    }

    public int GetIndex()
    {
        return index;
    }

    public void SetSelected(bool _selected)
    {
        selection_overlay.SetActive(_selected);
    }

    public void OnPointerDown(PointerEventData eventData)
    {
        // Handle event here AND in ancestors.
        // This allows event to bubble up and be handled by the AdvancesPanel, also. See https://coeurdecode.com/2015/10/20/bubbling-events-in-unity/
        ExecuteEvents.ExecuteHierarchy(transform.parent.gameObject, eventData, ExecuteEvents.pointerDownHandler);
    }

    public void OnPointerClick(PointerEventData _data)
    {
        CustomizePanel.instance.SelectEmblem(index);
    }
}
