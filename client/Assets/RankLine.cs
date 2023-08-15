using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class RankLine : MonoBehaviour
{
    public TMPro.TextMeshProUGUI indexText, nameText, nameOutlineText, amountText, amountOutlineText;
    public TMPro.TMP_ColorGradient playerGradientPreset, contactGradientPreset;
    public GameObject backgroundImage;

    static Color PLAYER_RANK_COLOR = new Color(0.424f, 1f, 0.318f);
    static Color CONTACT_RANK_COLOR = new Color(0f, 0.325f, 0.596f);
    static Color GENERIC_RANK_COLOR = new Color(0f, 0f, 0f);

    static Color PLAYER_AMOUNT_COLOR = new Color(0.424f, 1f, 0.318f);
    static Color CONTACT_AMOUNT_COLOR = new Color(0f, 0.325f, 0.596f);
    static Color GENERIC_AMOUNT_COLOR = new Color(0f, 0f, 0f);

    public enum RankLineFormat
    {
        INTEGER,
        MONEY
    }

    public void Init(RankEntryRecord _rank_entry_record, int _index, bool _display_index, RankLineFormat _format)
    {
        indexText.text = _display_index ? ("" + (_index + 1)) : "";
        backgroundImage.SetActive((_index % 2) == 1);
        string amountTextString = (_format == RankLineFormat.MONEY) ? ("$" + (_rank_entry_record.value / 100.0).ToString("N2")) : string.Format("{0:n0}", _rank_entry_record.value);

        switch (_rank_entry_record.type)
        {
            case RankEntryRecord.RankEntryType.PLAYER:
                nameText.gameObject.SetActive(false);
                nameOutlineText.gameObject.SetActive(true);
                amountText.gameObject.SetActive(false);
                amountOutlineText.gameObject.SetActive(true);
                //nameOutlineText.color = PLAYER_RANK_COLOR;
                //amountOutlineText.color = PLAYER_AMOUNT_COLOR;
                nameOutlineText.colorGradientPreset = playerGradientPreset;
                amountOutlineText.colorGradientPreset = playerGradientPreset;
                nameOutlineText.text = _rank_entry_record.name;
                amountOutlineText.text = amountTextString;
                break;
            case RankEntryRecord.RankEntryType.CONTACT:
                nameText.gameObject.SetActive(false);
                nameOutlineText.gameObject.SetActive(true);
                amountText.gameObject.SetActive(false);
                amountOutlineText.gameObject.SetActive(true);
                //nameText.color = CONTACT_RANK_COLOR;
                //amountText.color = CONTACT_AMOUNT_COLOR;
                nameOutlineText.colorGradientPreset = contactGradientPreset;
                amountOutlineText.colorGradientPreset = contactGradientPreset;
                nameOutlineText.text = _rank_entry_record.name;
                amountOutlineText.text = amountTextString;
                break;
            case RankEntryRecord.RankEntryType.GENERIC:
                nameText.gameObject.SetActive(true);
                nameOutlineText.gameObject.SetActive(false);
                amountText.gameObject.SetActive(true);
                amountOutlineText.gameObject.SetActive(false);
                nameText.color = GENERIC_RANK_COLOR;
                amountText.color = GENERIC_AMOUNT_COLOR;
                nameText.text = _rank_entry_record.name;
                amountText.text = amountTextString;
                break;
        }

        // Strikethrough the name text if active is false.
        nameText.fontStyle = _rank_entry_record.active ? TMPro.FontStyles.Normal : TMPro.FontStyles.Strikethrough;
        nameOutlineText.fontStyle = _rank_entry_record.active ? TMPro.FontStyles.Normal : TMPro.FontStyles.Strikethrough;
    }
}