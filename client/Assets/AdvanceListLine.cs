using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class AdvanceListLine : MonoBehaviour, IPointerDownHandler, IPointerClickHandler
{
    public enum Status
    {
        UNDEF,
        AVAILABLE_TO_RESEARCH,
        AVAILABLE_TO_BUY,
        UNAVAILABLE_TO_RESEARCH,
        UNAVAILABLE_TO_BUY,
        RESEARCHED,
        BOUGHT
    }

    public enum TargetStatus
    {
        NONE,
        TARGET,
        PREREQ
    }

    public Text text;
    public TMPro.TextMeshProUGUI bonusText;
    public Image icon, targetIcon, lineBackground;

    public TechData.Category category;
    public TechData techData;
    public bool purchasable;
    public bool available, acquired;
    public Status status;
    public TargetStatus targetStatus = TargetStatus.NONE;

    public void Init(TechData _techData)
    {
        // Record the TechData and its info.
        techData = _techData;
        category = techData.category;
        purchasable = (techData.category == TechData.Category.BASE_BUY) || (techData.category == TechData.Category.TECH_BUY) || (techData.category == TechData.Category.BIO_BUY) || (techData.category == TechData.Category.PSI_BUY);

        // Set icon
        switch (techData.category)
        {
            case TechData.Category.BASE_BUY:
            case TechData.Category.TECH_BUY:
            case TechData.Category.TECH:
                icon.sprite = AdvancesPanel.instance.icon_tech;
                break;
            case TechData.Category.BIO_BUY:
            case TechData.Category.BIO:
                icon.sprite = AdvancesPanel.instance.icon_bio;
                break;
            case TechData.Category.PSI_BUY:
            case TechData.Category.PSI:
                icon.sprite = AdvancesPanel.instance.icon_psi;
                break;
        }

        // Set the text
        text.text = techData.name;

        // Set the bonus text
        bonusText.text = AdvancesPanel.instance.GetAdvanceIconText(techData);
    }

    public void UpdateStatus()
    {
        // Determine whether the technology represented by this line has been acquired.
        acquired = (GameData.instance.GetTechCount(techData.ID) > 0);

        // Determine whether the technology represented by this line is currently available to be acquired.
        available = ((!acquired) && AdvancesPanel.instance.RequirementsMet(techData.ID));

        // Determine status
        if (purchasable)
        {
            if (acquired)
            {
                status = Status.BOUGHT;
            }
            else
            {
                if (available) {
                    status = Status.AVAILABLE_TO_BUY;
                } else {
                    status = Status.UNAVAILABLE_TO_BUY;
                }
            }
        }
        else
        {
            if (acquired)
            {
                status = Status.RESEARCHED;
            }
            else
            {
                if (available) {
                    status = Status.AVAILABLE_TO_RESEARCH;
                } else {
                    status = Status.UNAVAILABLE_TO_RESEARCH;
                }
            }
        }

        //Debug.Log("Updated status for advance " + techData.ID + " " + techData.name + " acquired: " + acquired + ", available: " + available + ", status: " + status);

        // Update the target icon.
        UpdateTargetIcon();
    }

    public void SetTargetStatus(TargetStatus _targetStatus)
    {
        targetStatus = _targetStatus;
        UpdateTargetIcon();
    }

    public void UpdateTargetIcon()
    {
        bool show_target_icon = (targetStatus == TargetStatus.TARGET);
        bool show_prereq_icon = (targetStatus == TargetStatus.PREREQ);

        if (show_target_icon)
        {
            targetIcon.gameObject.SetActive(true);
            targetIcon.sprite = AdvancesPanel.instance.targetSprite;
        }
        else if (show_prereq_icon)
        {
            targetIcon.gameObject.SetActive(true);
            targetIcon.sprite = AdvancesPanel.instance.targetReqSprite;
        }
        else
        {
            targetIcon.gameObject.SetActive(false);
        }
    }

    public void UpdateForLocalization()
    {
        // Set the text
        text.text = techData.name;
    }

    public void OnPointerDown(PointerEventData _data)
    {
    }

    public void OnPointerClick(PointerEventData _data)
    {
        // Activate this tech on the Advances Panel as well, so that the research button works.
        AdvancesPanel.instance.SelectAdvance(techData.ID, false);

        // Open the advance details panel to display info on this advance.
        AdvanceDetailsPanel.instance.Activate(techData.ID);
    }
}

public class AdvanceListLineComparer : IComparer<AdvanceListLine>
{
    public int Compare(AdvanceListLine a, AdvanceListLine b)
    {
        // Order by status first.
        if (a.status != b.status) {
            return (a.status < b.status) ? -1 : 1;
        }
        
        // Order by category
        if (a.category != b.category) {
            return (a.category < b.category) ? -1 : 1;
        }

        // Otherwise, order alphabetically.
        return String.Compare(a.techData.name, b.techData.name, StringComparison.OrdinalIgnoreCase);
    }
}
