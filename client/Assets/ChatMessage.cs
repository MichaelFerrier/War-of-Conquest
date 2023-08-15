using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class ChatMessage : MonoBehaviour
{
    public TMPro.TextMeshProUGUI textComponent;

    int sourceUserID, sourceNationID, channelID, mod_level;
    string sourceUsername, sourceNationName;

    public void SetMessage(int _sourceUserID, int _sourceNationID, string _sourceUsername, string _sourceNationName, int _sourceNationFlags, int _channelID, string _text, int _mod_level)
    {
        sourceUserID = _sourceUserID;
        sourceNationID = _sourceNationID;
        channelID = _channelID;
        mod_level = _mod_level;

        // Construct flare string for this nation.
        string flare = "";
        if ((_sourceNationFlags & (int)GameData.NationFlags.TOURNAMENT_FIRST_PLACE) != 0) flare += "<sprite=24>";
        if ((_sourceNationFlags & (int)GameData.NationFlags.TOURNAMENT_SECOND_PLACE) != 0) flare += "<sprite=25>";
        if ((_sourceNationFlags & (int)GameData.NationFlags.TOURNAMENT_THIRD_PLACE) != 0) flare += "<sprite=26>";
        if ((_sourceNationFlags & (int)GameData.NationFlags.ORB_OF_FIRE) != 0) flare += "<sprite=27>";

        textComponent.text = ((_sourceUserID == -1) ? "<i>" : ("<b>" + _sourceUsername + " " + LocalizationManager.GetTranslation("Generic Text/of_word") + " " + _sourceNationName + flare + ":</b> ")) + _text + ((_sourceUserID == -1) ? "</i>" : "");
    }
}
