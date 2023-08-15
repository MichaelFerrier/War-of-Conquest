using UnityEngine;
using UnityEngine.UI;
using System.Collections;
using I2.Loc;

public class MapFlagPanel : MonoBehaviour
{
    public static MapFlagPanel instance;

    public InputField entry_inputfield;

    private int blockX, blockZ;

    public MapFlagPanel()
    {
        instance = this;
    }

    public void Awake()
    {
        // Set entry field character limit
        entry_inputfield.characterLimit = GameGUI.MAX_MAP_FLAG_DESC_LENGTH;
    }

    public void OnEnable()
    {
        entry_inputfield.ActivateInputField();
    }

    public void Init(int _blockX, int _blockZ, string _text)
    {
        blockX = _blockX;
        blockZ = _blockZ;
        entry_inputfield.text = _text;
    }

    public void OnClick_Done()
    {
        string flagText = entry_inputfield.text;

        // Remove any control characters from the flag text
        flagText = GameGUI.RemoveControlCharacters(flagText);
        flagText = flagText.Replace("|", "/");

        // Send message to create or edit map flag.
        Network.instance.SendCommand("action=set_map_flag|x=" + blockX + "|y=" + blockZ + "|text=" + flagText);

        // Close the map flag panel.
        GameGUI.instance.CloseAllPanels();
    }

    public void OnClick_Close()
    {
        // Close the panel
        GameGUI.instance.CloseAllPanels();
    }
}
