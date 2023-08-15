using UnityEngine;
using System;
using System.Collections;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class AdvanceIcon : MonoBehaviour, IPointerDownHandler, IPointerClickHandler
{
    public int tech_ID;
    public TechData tech_data;
    public bool available, acquired;
    public int col = 0, row = 0;
    public Text nameText;
    public TMPro.TextMeshProUGUI bonusText;
    public GameObject bonusTextArea;
    private Sprite sprite, grayscale_sprite;
    private GameObject glow_object = null;
    private AdvanceIconGlow glow = null;

    public void Initialize(int _ID, TechData _tech_data, Sprite _sprite, Sprite _grayscale_sprite)
    {
        tech_ID = _ID;
        tech_data = _tech_data;

        sprite = _sprite;
        grayscale_sprite = _grayscale_sprite;

        gameObject.GetComponent<Image>().sprite = _sprite;

        // Name
        nameText.text = tech_data.name;

        // Bonuses
        //bonusText.text = AdvancesPanel.instance.GetAdvanceIconText(tech_data);
        bonusTextArea.gameObject.SetActive(false); // Hide the bonus text, for now. Will show for poster.
    }

    public void UpdateForTechnology(bool _acquired, bool _available)
    {
        acquired = _acquired;
        available = _available;

        //Debug.Log("tech " + tech_data.name + ", acquired: " + acquired + ", available: " + available);

        // Determine whether this icon should display as grayscale, for be not acquired and not available.
        bool grayscale = ((acquired == false) && (available == false));
        gameObject.GetComponent<Image>().sprite = grayscale ? grayscale_sprite : sprite;

        // Determine whether this icon should display a glow, for being available.
        if (_available && (glow == null))
        {
            glow_object = MemManager.instance.GetAdvanceIconGlowObject();
            glow = glow_object.GetComponent<AdvanceIconGlow>();

            glow_object.transform.localScale = new Vector3(1, 1, 1);
            glow_object.transform.SetParent(gameObject.transform.parent);
            glow_object.transform.SetAsFirstSibling();

            glow.Activate(this, (tech_data.default_price == 0) ? AdvanceIconGlow.Type.RESEARCHABLE : AdvanceIconGlow.Type.PURCHASABLE);
        }
        else if ((!_available) && (glow != null))
        {
            glow.Deactivate();
            glow = null;
        }
    }

    public void UpdateForLocalization()
    {
        nameText.text = tech_data.name;
    }

    public void OnPointerDown(PointerEventData eventData)
    {
        // Handle event here AND in ancestors.
        // This allows event to bubble up and be handled by the AdvancesPanel, also. See https://coeurdecode.com/2015/10/20/bubbling-events-in-unity/
        ExecuteEvents.ExecuteHierarchy(transform.parent.gameObject, eventData, ExecuteEvents.pointerDownHandler);
    }

    public void OnPointerClick(PointerEventData _data)
    {
        AdvancesPanel.instance.SelectAdvance(tech_ID, false);
    }
}
