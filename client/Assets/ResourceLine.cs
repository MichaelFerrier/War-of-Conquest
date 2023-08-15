using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;

public class ResourceLine : MonoBehaviour
{
    public TMPro.TextMeshProUGUI titleText, techText, bioText, psiText, energyText, manpowerText, xpText;
    public GameObject amountsPanel, requirementsPanel;
    public Image expandButtonImage;
    public int objectID, techID;
    private ObjectData objectData;
    private TechData techData;
    private bool expanded;
    public int valTech, valBio, valPsi, valEnergy, valManpower, valXP, count;
    public Sprite iconPlusSprite, iconMinusSprite;
    public bool requirementsMet;

    public void Init(int _objectID, bool _expanded)
    {
        objectID = _objectID;
        expanded = _expanded;

        // Get the object and tech data.
        objectData = ObjectData.GetObjectData(objectID);
        techData = TechData.GetTechData(objectData.techID);

        // Determine whether this resource's requirements have been met.
        requirementsMet = AdvancesPanel.instance.RequirementsMet(objectData.techID);

        // Show the appropriate panel based on whether the resource's requirements are met.
        amountsPanel.SetActive(requirementsMet);
        requirementsPanel.SetActive(!requirementsMet);

        // Initialize bonus values and count.
        valTech = valBio = valPsi = valEnergy = valManpower = valXP = 0;
        count = 0;

        // Set the image for the expand button based on its state.
        expandButtonImage.sprite = expanded ? iconMinusSprite : iconPlusSprite;
    }

    public void AddObject(int _blockX)
    {
        // Increment count of objects of this type.
        count++;

        // Determine the object's position.
        float position = objectData.GetPosition(_blockX);

        // Apply each of the object's bonuses.
        ApplyBonus(techData.bonus_type_1, techData.GetBonusVal(1), techData.GetBonusValMax(1), position);
        ApplyBonus(techData.bonus_type_2, techData.GetBonusVal(2), techData.GetBonusValMax(2), position);
        ApplyBonus(techData.bonus_type_3, techData.GetBonusVal(3), techData.GetBonusValMax(3), position);
    }

    public void Finish()
    {
        // Set title text.
        titleText.text = objectData.name + " (" + count + ")";

        // Display each of the resource's bonus totals.
        DisplayBonus(valTech, techText, false);
        DisplayBonus(valBio, bioText, false);
        DisplayBonus(valPsi, psiText, false);
        DisplayBonus(valEnergy, energyText, false);
        DisplayBonus(valManpower, manpowerText, false);
        DisplayBonus(valXP, xpText, true);
    }

    private void ApplyBonus(TechData.Bonus _bonus_type, int _bonus_val, int _bonus_val_max, float _position)
    {
        if (_bonus_val_max > 0) {
            _bonus_val += (int)(((_bonus_val_max - _bonus_val) * _position));
        }
        
        switch (_bonus_type)
        {
            case TechData.Bonus.TECH: valTech += _bonus_val; break;
            case TechData.Bonus.BIO: valBio += _bonus_val; break;
            case TechData.Bonus.PSI: valPsi += _bonus_val; break;
            case TechData.Bonus.ENERGY_RATE: valEnergy += _bonus_val; break;
            case TechData.Bonus.MANPOWER_RATE: valManpower += _bonus_val; break;
            case TechData.Bonus.XP_MULTIPLIER: valXP += _bonus_val; break;
        }
    }

    public void OnClick_ExpandButton()
    {
        // Toggle whether this resource is expanded in the list.
        NationPanel.instance.SetExpandedObjectID(expanded ? -1 : objectID);
    }

    public static void DisplayBonus(int _bonus_value, TMPro.TextMeshProUGUI _text, bool _percent)
    {
        if (_bonus_value > 0)
        {
            _text.text = "+" + string.Format("{0:n0}", _bonus_value) + (_percent ? "%" : "");
            _text.color = new Color(0, 1, 0);
        }
        else if (_bonus_value < 0)
        {
            _text.text = "-" + string.Format("{0:n0}", _bonus_value) + (_percent ? "%" : "");
            _text.color = new Color(1, 0, 0);
        }
        else
        {
            _text.text = "";
        }
    }
}
