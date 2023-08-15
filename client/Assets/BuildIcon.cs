using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class BuildIcon : MonoBehaviour
{
    public TMPro.TextMeshProUGUI nameText;
    public Text manpowerCostText, energyCostText, buildTimeText;
    public int ID = -1;
    public Image image;

	public void Init(BuildData _build_data)
    {
   		// Determine the structure's energy burn rate for this nation.
		float energy_burn_rate = (1.0f - ((_build_data.type == BuildData.Type.WALL) ? GameData.instance.wallDiscount : GameData.instance.structureDiscount)) * _build_data.energy_burn_rate;

        // GB-Localization
        // Set name and costs texts
        nameText.text = _build_data.name;
        manpowerCostText.text = string.Format("{0:n0}", _build_data.manpower_cost);
        energyCostText.text = string.Format("{0:n0}", energy_burn_rate) + "/" + I2.Loc.LocalizationManager.GetTranslation("time_hour");
        buildTimeText.text = GameData.instance.GetDurationText(_build_data.build_time);

        // Record build ID
        ID = _build_data.ID;

        Debug.Log("Build icon init ID: " + ID);
    }
}
