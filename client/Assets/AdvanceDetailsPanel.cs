using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using I2.Loc;

public class AdvanceDetailsPanel : MonoBehaviour, IPointerDownHandler
{
    public static AdvanceDetailsPanel instance;

    public TMPro.TextMeshProUGUI title_text, advanceDescriptionText;
    public int advanceID = -1;
    public ScrollRect descriptionScrollRect;
    public GameObject button_area_object, target_button_object, research_button_object, advance_point_icon, advance_point_grayscale_icon, credit_icon, credit_grayscale_icon;
    public Text research_button_text, research_button_cost_text, target_button_text;
    public Button research_button;

	public AdvanceDetailsPanel()
    {
        instance = this;	
	}

    public void Activate(int _advanceID)
    {
        // Record the advance ID
        advanceID = _advanceID;

        if (!gameObject.activeInHierarchy)
        {
            // Open the panel
            GameGUI.instance.OpenAdvanceDetailsDialog();
        }

        // Get the TechData for the given advance
        TechData techData = TechData.GetTechData(advanceID);

        // Display the tech's name as the title.
        title_text.text = techData.name;

        // Get the tech's description text.
        string description_text = AdvancesPanel.instance.GetAdvanceDescriptionText(techData, false);

        // Display the description text.
        advanceDescriptionText.text = description_text;

        // Get the list line corresponding to this advance.
        AdvanceListLine advance_list_line = AdvancesPanel.instance.advanceListLinesHash.ContainsKey(advanceID) ? AdvancesPanel.instance.advanceListLinesHash[advanceID] : null;

        //Debug.Log("Advance details panel for " + techData.ID + " " + techData.name + ": acquired: " + advance_list_line.acquired + ", available: " + advance_list_line.available);

        // Update research and target buttons
        if ((advance_list_line == null) || (advance_list_line.acquired))
        {
            // If this advance is already acquired, do not show the research or target buttons at all.
            button_area_object.SetActive(false);
        }
        else
        {
            // This advance has not yet been acquired, so show the button.
            button_area_object.SetActive(true);

            if (advance_list_line.available)
            {
                // Show the research button, not the target button.
                research_button_object.SetActive(true);
                target_button_object.SetActive(false);

                if (techData.default_price == 0)
                {
                    // GB:Localization

                    string _research_button_text = LocalizationManager.GetTranslation("Advances Panel/research_button_text"); // "Research"
                    research_button_text.text = _research_button_text;
                    research_button_cost_text.text = "1";
                    credit_icon.SetActive(false);
                    credit_grayscale_icon.SetActive(false);
                    Debug.Log("credit icon deactivated. active: " + credit_icon.activeSelf);

                    // Enable button (make interactable) only if there are enough advance points.
                    if (GameData.instance.advance_points >= 1)
                    {
                        research_button.interactable = true;
                        advance_point_icon.SetActive(true);
                        advance_point_grayscale_icon.SetActive(false);
                    }
                    else 
                    {
                        research_button.interactable = false; 
                        advance_point_icon.SetActive(false);
                        advance_point_grayscale_icon.SetActive(true);
                    }
                }
                else
                {
                    // GB:Localization
                    string buy_button_text = LocalizationManager.GetTranslation("buy_button_text");
                    research_button_text.text = buy_button_text;
                    research_button_cost_text.text = string.Format("{0:n0}", GameData.instance.GetPrice(techData.ID));
                    advance_point_icon.SetActive(false);
                    advance_point_grayscale_icon.SetActive(false);

                    // Enable button (make interactable) (regardess of whether we have the credits; they can be bought).
                    research_button.interactable = true;
                    credit_icon.SetActive(true);
                    credit_grayscale_icon.SetActive(false);
                    Debug.Log("credit icon activated. active: " + credit_icon.activeSelf);
                }
            }
            else
            {
                // Show the target button, not the research button.
                target_button_object.SetActive(true);
                research_button_object.SetActive(false);

                // Set the text of the target button.
                if (GameData.instance.targetAdvanceID == advanceID) {
                    target_button_text.text = LocalizationManager.GetTranslation("Advances Panel/target_clear");
                } else {
                    target_button_text.text = LocalizationManager.GetTranslation("Advances Panel/target_set");
                }
            }
        }
    }

    public void UpdateForTechnologies()
    {
        if (gameObject.activeInHierarchy) {
            Activate(advanceID);
        }
    }

    public void UpdateForSetTarget()
    {
        if (gameObject.activeInHierarchy) {
            Activate(advanceID);
        }
    }

    public void OnPointerDown(PointerEventData _eventData)
    {
        // Determine whether link text has been clicked.
        int link_index = TMPro.TMP_TextUtilities.FindIntersectingLink(advanceDescriptionText, Input.mousePosition, null);

        if (link_index != -1)
        {
            if (GameGUI.instance.linkManager.link_types[link_index] == LinkManager.LinkType.BUILD)
            {
                GameGUI.instance.OpenBuildInfoDialog(-1, -1, GameGUI.instance.linkManager.link_ids[link_index]);
            }
            else if (GameGUI.instance.linkManager.link_types[link_index] == LinkManager.LinkType.TECH)
            {
                Activate(GameGUI.instance.linkManager.link_ids[link_index]);
            }
            else if (GameGUI.instance.linkManager.link_types[link_index] == LinkManager.LinkType.STAT)
            {
                StatDetailsPanel.instance.ActivateForBonus((TechData.Bonus)(GameGUI.instance.linkManager.link_ids[link_index]));
            }
        }
    }

    public void OnClick_TargetButton()
    {
        if (advanceID == GameData.instance.targetAdvanceID)
        {
            // Send message to server to clear target.
            Network.instance.SendCommand("action=set_target|ID=-1");
        }
        else
        {
            // Send message to server to set target.
            Network.instance.SendCommand("action=set_target|ID=" + advanceID);
        }
    }
}
