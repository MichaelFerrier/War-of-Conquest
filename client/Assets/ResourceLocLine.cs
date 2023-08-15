using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class ResourceLocLine : MonoBehaviour
{
    public TMPro.TextMeshProUGUI locationText, techText, bioText, psiText, energyText, manpowerText, xpText;

    ObjectData objectData;
    TechData techData;
    int blockX, blockZ;

    public void Init(int _objectID, int _blockX, int _blockZ, bool _requirementsMet)
    {
        blockX = _blockX;
        blockZ = _blockZ;

        // Get the object and tech data.
        objectData = ObjectData.GetObjectData(_objectID);
        techData = TechData.GetTechData(objectData.techID);

        // Set the map location text.
        locationText.text = MapView.instance.GetMapLocationText(_blockX, _blockZ, true);

        // Determine the object's position.
        float position = objectData.GetPosition(_blockX);

        // Clear all text fields.
        techText.text = "";
        bioText.text = "";
        psiText.text = "";
        energyText.text = "";
        manpowerText.text = "";
        xpText.text = "";

        if (_requirementsMet)
        {
            // Apply each of the object's bonuses.
            ApplyBonus(techData.bonus_type_1, techData.GetBonusVal(1), techData.GetBonusValMax(1), position);
            ApplyBonus(techData.bonus_type_2, techData.GetBonusVal(2), techData.GetBonusValMax(2), position);
            ApplyBonus(techData.bonus_type_3, techData.GetBonusVal(3), techData.GetBonusValMax(3), position);
        }
    }

    public void OnPointerClick()
    {
        // If we're not looking at the mainland map, do nothing.
        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        // Close the nation panel.
        GameGUI.instance.CloseActiveGamePanel();

        // Send message to server, to set view to the map location of this resource.
        Network.instance.SendCommand("action=event_center_on_block|blockX=" + blockX + "|blockY=" + blockZ, true);
    }

    private void ApplyBonus(TechData.Bonus _bonus_type, int _bonus_val, int _bonus_val_max, float _position)
    {
        if (_bonus_val_max > 0) {
            _bonus_val += (int)(((_bonus_val_max - _bonus_val) * _position));
        }
        
        switch (_bonus_type)
        {
            case TechData.Bonus.TECH: ResourceLine.DisplayBonus(_bonus_val, techText, false); break;
            case TechData.Bonus.BIO: ResourceLine.DisplayBonus(_bonus_val, bioText, false); break;
            case TechData.Bonus.PSI: ResourceLine.DisplayBonus(_bonus_val, psiText, false); break;
            case TechData.Bonus.ENERGY_RATE: ResourceLine.DisplayBonus(_bonus_val, energyText, false); break;
            case TechData.Bonus.MANPOWER_RATE: ResourceLine.DisplayBonus(_bonus_val, manpowerText, false); break;
            case TechData.Bonus.XP_MULTIPLIER: ResourceLine.DisplayBonus(_bonus_val, xpText, false); break;
        }
    }
}
