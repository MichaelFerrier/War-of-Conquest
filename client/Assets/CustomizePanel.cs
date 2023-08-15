using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using I2.Loc;

public class CustomizePanel : MonoBehaviour
{
    public static CustomizePanel instance;

    public EmblemIcon emblemIconPrefab;
    public RectTransform contentRectTransform;
    public TMPro.TextMeshProUGUI changeButtonText;

    public const int RESTRICTED_EMBLEM_START_INDEX = 120;

    public const int NUM_NATION_COLOR_OPTIONS = 30;
    public Toggle[] nation_color_toggles = new Toggle[NUM_NATION_COLOR_OPTIONS];

    public const int NUM_EMBLEM_COLOR_OPTIONS = 7;
    public Toggle[] emblem_color_toggles = new Toggle[NUM_EMBLEM_COLOR_OPTIONS];

    int selEmblemIndex = -1;
    int nationColorR, nationColorG, nationColorB;

    public CustomizePanel()
    {
        instance = this;
    }

    public void InfoEventReceived()
    {
        Reset();

        // Set the change button's text, with cost.
        changeButtonText.text = string.Format("{0}  ({1}<sprite=2>)",
        I2.Loc.LocalizationManager.GetTranslation("Customize Panel/make_change"),
        GameData.instance.customizeCost);

        // Show only those emblems that are available to this player.
        ShowAvailableEmblems();
    }

    // Show only those emblems that area available to this player.
    public void ShowAvailableEmblems()
    {
        EmblemIcon cur_icon;

        foreach(Transform cur_icon_transform in contentRectTransform)
        {
            cur_icon = cur_icon_transform.gameObject.GetComponent<EmblemIcon>();
            //Debug.Log("Cur icon index: " + cur_icon.GetIndex() + ", player info: " + GameData.instance.player_info);
            cur_icon.gameObject.SetActive((cur_icon.GetIndex() < RESTRICTED_EMBLEM_START_INDEX) || (GameData.instance.player_info.Contains("<emblem index=\"" + cur_icon.GetIndex() + "\">")) || (GameData.instance.userIsAdmin)); 
        }
    }

    // Reset values to the actual values of the player's nation.
    public void Reset()
    {
        int closestIndex = -1;
        float curDif, closestDif = 1000000f;
        Color curColor;

        // Determine which nation color toggle best matches the nation's color.
        for (int i = 0; i < NUM_NATION_COLOR_OPTIONS; i++) 
        {
            curColor = nation_color_toggles[i].gameObject.transform.GetChild(0).GetComponent<Image>().color;
            curDif = Mathf.Abs(curColor.r - GameData.instance.nationColor.r) + Mathf.Abs(curColor.g - GameData.instance.nationColor.g) + Mathf.Abs(curColor.b - GameData.instance.nationColor.b);

            if (curDif < closestDif) 
            {
                closestDif = curDif;
                closestIndex = i;
            }
        }

        // Turn on only the nation color toggle best matching the nation's color.
        for (int i = 0; i < NUM_NATION_COLOR_OPTIONS; i++) {
            nation_color_toggles[i].isOn = (i == closestIndex);
        }

        // Turn on only the emblem color toggle that matches the nation's emblem color.
        for (int i = 0; i < NUM_EMBLEM_COLOR_OPTIONS; i++) {
            emblem_color_toggles[i].isOn = (i == ((int)GameData.instance.emblemColor));
        }

        // Select the nation's emblem index.
        SelectEmblem(GameData.instance.emblemIndex);
    }

    public void ChangeAppearanceLocally(bool _permanent, bool _update_map_view)
    {
        NationData nation_data = GameData.instance.nationTable[GameData.instance.nationID];

        // Nation color
        for (int i = 0; i < NUM_NATION_COLOR_OPTIONS; i++)
        {
            if (nation_color_toggles[i].isOn)
            {
                nation_data.r = nation_color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.r;
                nation_data.g = nation_color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.g;
                nation_data.b = nation_color_toggles[i].gameObject.transform.GetChild(0).gameObject.GetComponent<Image>().color.b;

                if (_permanent) 
                {
                    GameData.instance.nationColor.r = nation_data.r;
                    GameData.instance.nationColor.g = nation_data.g;
                    GameData.instance.nationColor.b = nation_data.b;

                    // The player's nation's color has been set.
                    GameGUI.instance.NationColorChanged();
                }
                break;
            }
        }

        // Nation emblem
        nation_data.emblem_index = selEmblemIndex;
        nation_data.DetermineEmblemUV();

        if (_permanent)  {
            GameData.instance.emblemIndex = selEmblemIndex;
        }

        // Nation emblem color
        for (int i = 0; i < NUM_EMBLEM_COLOR_OPTIONS; i++)
        {
            if (emblem_color_toggles[i].isOn)
            {
                nation_data.emblem_color = (NationData.EmblemColor)i;

                if (_permanent)  {
                    GameData.instance.emblemColor = (NationData.EmblemColor)i;
                }

                break;
            }
        }

        if (_update_map_view)
        {
            // Have the map update all patches, to display the change in the player's nation's appearance.
            MapView.instance.QueueAllPatchesModified(false);
        }
     }

    public void InitEmblems(Texture2D _tex, Color32[] _pix, float[,] _emblemData)
    {
        int emblem_index = 0, x, y, x0, y0;
        bool emblem_exists;
        EmblemIcon emblem_icon;
        GameObject emblem_icon_object;
        
        // Clear the emblem scroll rect's content
        while (contentRectTransform.gameObject.transform.childCount > 0) 
        {
            emblem_icon_object = contentRectTransform.gameObject.transform.GetChild(0).gameObject;
            emblem_icon_object.transform.SetParent(null);
            GameObject.Destroy(emblem_icon_object);
        }

        // Create icon representing no emblem
        emblem_icon = (EmblemIcon)Instantiate(emblemIconPrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity);
        emblem_icon.Init(-1, null, 0, 0, MapView.EMBLEM_DIM);
        emblem_icon.gameObject.transform.SetParent(contentRectTransform.gameObject.transform);
        emblem_icon.gameObject.transform.localScale = new Vector3(1, 1, 1);

        for (int ey = 0; ey < (_tex.height / MapView.EMBLEM_DIM); ey++)
        {
            for (int ex = 0; ex < (_tex.width / MapView.EMBLEM_DIM); ex++)
            {
                // Determine whether an emblem exists at this location, by checking pixels in an X shape spanning the emblem's area.
                emblem_exists = false;
                x0 = ex * MapView.EMBLEM_DIM;
                y0 = ey * MapView.EMBLEM_DIM;
                for (y = 0; y < MapView.EMBLEM_DIM; y++)
                {
                    if ((_emblemData[x0 + y, y0 + y] != 1f) || (_emblemData[x0 + MapView.EMBLEM_DIM - 1 - y, y0 + y] != 1f)) 
                    {
                        emblem_exists = true;
                        break;
                    }
                }

                if (emblem_exists)
                {
                    // Create an icon or this emblem.
                    emblem_icon = (EmblemIcon)Instantiate(emblemIconPrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity);
                    emblem_icon.Init(emblem_index, _tex, x0, y0, MapView.EMBLEM_DIM);
                    emblem_icon.gameObject.transform.SetParent(contentRectTransform.gameObject.transform);
                    emblem_icon.gameObject.transform.localScale = new Vector3(1, 1, 1);
                }
                
                // Increment emblem index.
                emblem_index++;
            }
        }
    }

    public void SelectEmblem(int _index)
    {
        selEmblemIndex = _index;

        EmblemIcon cur_icon;
        foreach(Transform cur_icon_transform in contentRectTransform)
        {
            cur_icon = cur_icon_transform.gameObject.GetComponent<EmblemIcon>();
            cur_icon.SetSelected(cur_icon.GetIndex() == _index); 
        }
    }

    public void OnClick_Close()
    {
        // Restore the nation's actual appearance values to this panel.
        Reset();

        // Set the nation's local appearance on this client's map to match the values in this panel.
        ChangeAppearanceLocally(false, true);

        // Close this panel.
        GameGUI.instance.CloseAllPanels();
    }

    public void OnClick_Demo()
    {
        // Set the nation's local appearance on this client's map to match the values in this panel.
        ChangeAppearanceLocally(false, true);

        // Close this panel.
        GameGUI.instance.CloseAllPanels();

        // Close the open game panel as well.
        GameGUI.instance.CloseActiveGamePanel();
    }

    public void OnClick_Change()
    {
        if (GameData.instance.credits < GameData.instance.customizeCost)
        {
            // Bring up UI for buying credits.
            GameGUI.instance.RequestBuyCredits();
        }
        else 
        {
            // Set the nation's local appearance on this client's map to match the values in this panel.
            // Dont update the map view, though -- rely on broadcast of nation_data event, which all online players receive.
            ChangeAppearanceLocally(true, false);

            // Close this panel.
            GameGUI.instance.CloseAllPanels();

            // Close the open game panel as well.
            GameGUI.instance.CloseActiveGamePanel();

            // Send customize appearance event to the server.
            NationData nation_data = GameData.instance.nationTable[GameData.instance.nationID];
            Network.instance.SendCommand("action=customize_appearance|color_r=" + (int)(nation_data.r * 255f + 0.5f) + "|color_g=" + (int)(nation_data.g * 255f + 0.5f) + "|color_b=" + (int)(nation_data.b * 255f + 0.5f) + "|emblem_index=" + nation_data.emblem_index + "|emblem_color=" + (int)nation_data.emblem_color);
        }
    }
}
