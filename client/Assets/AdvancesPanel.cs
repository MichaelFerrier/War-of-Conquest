using UnityEngine;
using UnityEngine.Analytics;
using UnityEngine.UI;
using UnityEngine.EventSystems;
using System;
using System.Collections;
using System.Collections.Generic;
using I2.Loc;

public class AdvancesPanel : MonoBehaviour, IPointerDownHandler
{
    public enum Mode
    {
        Tree,
        List
    }

    private const int VERTICAL_MARGIN = 60;
    private const int HORIZONTAL_MARGIN = 70;
    private const int SPACING = 90;
    private const float ICON_INIT_SIZE = 0.66f;
    private const float ICON_GROW_SIZE = 1.0f;
    private const float ICON_GROW_DURATION = 0.1f;
    private const float ICON_SHRINK_DURATION = 0.05f;

    private const int INFO_TEXT_LARGE = 15;
    private const int INFO_TEXT_MEDIUM = 13;
    private const int INFO_TEXT_SMALL = 12;

    private const string color_tag_inc = "<color=#00ff00ff>"; // <color=lime>

    public static AdvancesPanel instance;

    public Canvas canvas;
    public ZoomRect zoomRect;
    public AdvanceIcon advanceIconPrefab;
    public AdvanceLine advanceLinePrefab;
    public AdvanceLine purchasableLinePrefab;
    public TMPro.TextMeshProUGUI advanceDescriptionText;

    public Sprite icon_tech, icon_bio, icon_psi, icon_tech_grayscale, icon_bio_grayscale, icon_psi_grayscale;
    public int selectedAdvanceID = -1, advanceIDToSelect = -1;

    public ScrollRect scrollRect, descriptionScrollRect;
    public RectTransform contentObjectRectTransform, advancePanelRectTransform;
    public Image contentBGImage;
    public GameObject treeContentArea, listContentArea;
    public GameObject content_object, button_area_object, research_button_object, advance_point_icon, advance_point_grayscale_icon, credit_icon, credit_grayscale_icon;
    public Image modeButtonImage;
    public Sprite modeTreeSprite, modeListSprite, lightLineSprite, darkLineSprite, targetSprite, targetReqSprite;
    public Text research_button_text, research_button_cost_text;
    public Button research_button;
    public List<AdvanceIcon> advance_icon_objects;

    public GameObject advanceListLinePrefab;
    public GameObject titleAvailToReseach, titleAvailToBuy, titleUnavailToResearch, titleUnavailToBuy, titleResearched, titleBought;
    public GameObject listContentObject;
    
    public bool screenshotFormat = false;

    private string time_remaining_string;
    bool initialized = false;
    public Mode mode = Mode.List;
    bool treeRequiresUpdate = true, listRequiresUpdate = true;
    public Dictionary<int, AdvanceIcon> advanceIconsHash = new Dictionary<int, AdvanceIcon>();
    List<AdvanceListLine> advanceListLines = new List<AdvanceListLine>();
    public Dictionary<int, AdvanceListLine> advanceListLinesHash = new Dictionary<int, AdvanceListLine>();
    List<AdvanceListLine> targetPreReqs = new List<AdvanceListLine>();

    public AdvancesPanel()
    {
        instance = this;
    }

    public void Start()
    {
        SetMode(mode);
    }

    public void OnEnable()
    {
        // Set the ZoomRect's reference size for the content
        zoomRect.SetContentRefSize(contentObjectRectTransform.sizeDelta);

        // Update the UI layout so that its numbers are correct before getting to the below code.
        Canvas.ForceUpdateCanvases();

        // Have the ZoomRect determine the min scale for the content image.
        zoomRect.DetermineMinScale();

        // Update the appropriate subpanel for being shown, if necessary.
        UpdateForShow();

        if ((selectedAdvanceID == -1) && (advanceIDToSelect != -1)) {
            SelectAdvance(advanceIDToSelect, true);
        }

        // Adjust the size of the description text for the current resolution.
        StartCoroutine(ResolutionChanged_Coroutine());
    }

    // This is necessary to keep the description scrollbar from flashing on briefly when the panel first appears.
    public IEnumerator ResolutionChanged_Coroutine()
    {
        advanceDescriptionText.gameObject.GetComponent<LayoutElement>().preferredWidth = 325;
        yield return new WaitForFixedUpdate();
        ResolutionChanged();
    }

    public void Update()
    {
        if (selectedAdvanceID != -1)
        {
            TechData tech_data = TechData.GetTechData(selectedAdvanceID);
            if ((tech_data != null) && (tech_data.duration_type == TechData.Duration.TEMPORARY) && (GameData.instance.GetTechCount(selectedAdvanceID) > 0))
            {
                String new_time_remaining_string = GameData.instance.ConstructTimeRemainingString(selectedAdvanceID);
                
                if (String.Equals(new_time_remaining_string, time_remaining_string) == false)
                {
                    time_remaining_string = new_time_remaining_string;
                    SelectAdvance(selectedAdvanceID, true);
                }
            }
        }
    }

    public void SetMode(Mode _mode)
    {
        // Record the new mode.
        mode = _mode;

        // Activate the appropriate content area.
        treeContentArea.SetActive(mode == Mode.Tree);
        listContentArea.SetActive(mode == Mode.List);

        // Set the image on the mode button to represent the opposite mode.
        modeButtonImage.sprite = (mode == Mode.Tree) ? modeListSprite : modeTreeSprite;

        // Update the appropriate subpanel for being shown, if necessary.
        UpdateForShow();
    }

    public void Initialize()
    {
        // Only initialize once.
        if (initialized) {
            return;
        }

        // Record that initialization has taken place.
        initialized = true;

        int num_rows = 0, num_cols = 0;
        TechData tech_data;
        AdvanceIcon new_icon;

        int[] ctg_num_cols = new int[7] {0,0,0,0,0,0,0};
        int[] category_start_x = new int[7] {0,0,0,0,0,0,0};
        int[,] col_heights = new int[7, 20] { {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1}, {-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1} };

        // Create array of advance icons, one per tech.
        advance_icon_objects = new List<AdvanceIcon>();

        // Create an AdvanceIcon for each advance (technology), other than the initial advances, random advances, and landscape object techs, which are hidden.
        foreach(KeyValuePair<int, TechData> entry in TechData.techs)
		{
            tech_data = entry.Value;

            // Skip technologies that are added initially to starting nations.
            if (tech_data.initial) {
                continue;
            }

            // Skip technologies that do not fall into a display category.
            if (tech_data.category == TechData.Category.UNDEF) {
                continue;
            }

            // Instantiate an AdvanceIcon to represent this advance.
            new_icon = (AdvanceIcon)Instantiate(advanceIconPrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity);
            advance_icon_objects.Add(new_icon);

            // Determine sprites for this icon.
            Sprite sprite, sprite_grayscale;
            switch (entry.Value.category)
            {
                case TechData.Category.TECH:
                case TechData.Category.TECH_BUY:
                    sprite = icon_tech;
                    sprite_grayscale = icon_tech_grayscale;
                    break;
                case TechData.Category.BIO:
                case TechData.Category.BIO_BUY:
                    sprite = icon_bio;
                    sprite_grayscale = icon_bio_grayscale;
                    break;
                case TechData.Category.PSI:
                case TechData.Category.PSI_BUY:
                    sprite = icon_psi;
                    sprite_grayscale = icon_psi_grayscale;
                    break;
                default:
                    sprite = icon_tech;
                    sprite_grayscale = icon_tech_grayscale;
                    break;
            }

            // Initialize the AdvanceIcon, and connect it with its corresponding TechData.
            new_icon.Initialize(entry.Key, entry.Value, sprite, sprite_grayscale);
            advanceIconsHash[entry.Key] = new_icon;
        }

        // Sort the array of AdvanceIcons by the "order", then the ID, of the advances.
        advance_icon_objects.Sort(delegate(AdvanceIcon _icon1, AdvanceIcon _icon2) {
                    return (_icon1.tech_data.order < _icon2.tech_data.order) ? -1 : ((_icon1.tech_data.order > _icon2.tech_data.order) ? 1 : (_icon1.tech_data.ID < _icon2.tech_data.ID) ? -1 : ((_icon1.tech_data.ID > _icon2.tech_data.ID) ? 1 : 0));
                  });

        int new_row, new_col, prereq_ID;
        TechData prereq;
        bool change_made = true, first_iter = true;
        while (change_made)
        {
            change_made = false;

            foreach (AdvanceIcon cur_icon in advance_icon_objects)
            {
                new_row = cur_icon.row;
                new_col = cur_icon.col;

                if (first_iter &&
                    ((cur_icon.tech_data.category == TechData.Category.BASE_BUY) || 
                     (cur_icon.tech_data.category == TechData.Category.TECH_BUY) || 
                     (cur_icon.tech_data.category == TechData.Category.BIO_BUY) || 
                     (cur_icon.tech_data.category == TechData.Category.PSI_BUY)))
                {
                    new_col = 0;
                    //Debug.Log("col_heights[" + cur_icon.tech_data.category + "," + new_col + "]: " + (col_heights[(int)(cur_icon.tech_data.category), new_col] + 1) + ", cur_icon.row: " + cur_icon.row);
                    new_row = col_heights[(int)(cur_icon.tech_data.category), new_col] + 1;
                }
                
                // Adjust position for first prerequisite.
                prereq_ID = cur_icon.tech_data.prerequisite_tech_1;
                if (prereq_ID != -1) 
                {
                    prereq = TechData.GetTechData(prereq_ID);

                    // Get the icon corresponding to this prereq advance.
                    AdvanceIcon prereq_icon = AdvancesPanel.instance.advanceIconsHash[prereq_ID];

                    new_row = Mathf.Max(new_row, prereq_icon.row + 1);
                    if (cur_icon.tech_data.category == prereq.category) new_col = Mathf.Max(new_col, prereq_icon.col);
                }

                // Adjust position for second prerequisite.
                prereq_ID = cur_icon.tech_data.prerequisite_tech_2;
                if (prereq_ID != -1) 
                {
                    prereq = TechData.GetTechData(prereq_ID);

                    // Get the icon corresponding to this prereq advance.
                    AdvanceIcon prereq_icon = AdvancesPanel.instance.advanceIconsHash[prereq_ID];

                    new_row = Mathf.Max(new_row, prereq_icon.row + 1);
                    if (cur_icon.tech_data.category == prereq.category) new_col = Mathf.Max(new_col, prereq_icon.col);
                }

                if (first_iter)
                {
                    // Advance to a column that is not already in use at this row.
                    //Debug.Log("Tech ID: " + cur_icon.tech_data.ID + ", ctg " + cur_icon.tech_data.category + ", new_col: " + new_col + ", new_row: " + new_row);
                    //Debug.Log("     col_height: " + col_heights[(int)(cur_icon.tech_data.category), new_col] + ", will increase col: " + (col_heights[(int)(cur_icon.tech_data.category), new_col] >= new_row) + ".");
                    while (col_heights[(int)(cur_icon.tech_data.category), new_col] >= new_row) {
                        new_col++;
                    }
                }

                // If this advance icon is changing position, record its new position and record that a change has been made.
                if (first_iter || (new_col != cur_icon.col) || (new_row != cur_icon.row))
                {
                    cur_icon.col = new_col;
                    cur_icon.row = new_row;
                    col_heights[(int)(cur_icon.tech_data.category), new_col] = Mathf.Max(col_heights[(int)(cur_icon.tech_data.category), new_col], new_row);
                    ctg_num_cols[(int)(cur_icon.tech_data.category)] = Mathf.Max(ctg_num_cols[(int)(cur_icon.tech_data.category)], new_col + 1);
                    num_rows = Mathf.Max(num_rows, new_row + 1);
                    change_made = true;
                }
                //Debug.Log(cur_icon.tech_data.ID + ": " + cur_icon.tech_data.name + ": pos " + new_col + "," + new_row);
            }

            first_iter = false;
        }

        // Determine total number of columns.
        num_cols = ctg_num_cols[(int)(TechData.Category.TECH)] + ctg_num_cols[(int)(TechData.Category.BIO)] + ctg_num_cols[(int)(TechData.Category.PSI)] + 4;

        // Determine x offsets of each advance category.
        category_start_x[(int)(TechData.Category.BASE_BUY)] = HORIZONTAL_MARGIN;
        category_start_x[(int)(TechData.Category.TECH_BUY)] = HORIZONTAL_MARGIN + (1 * SPACING);
        category_start_x[(int)(TechData.Category.TECH)] = HORIZONTAL_MARGIN + (2 * SPACING);
        category_start_x[(int)(TechData.Category.BIO_BUY)] = category_start_x[(int)(TechData.Category.TECH)] + (ctg_num_cols[(int)(TechData.Category.TECH)] * SPACING);
        category_start_x[(int)(TechData.Category.BIO)] = category_start_x[(int)(TechData.Category.BIO_BUY)] + (1 * SPACING);
        category_start_x[(int)(TechData.Category.PSI_BUY)] = category_start_x[(int)(TechData.Category.BIO)] + (ctg_num_cols[(int)(TechData.Category.BIO)] * SPACING);
        category_start_x[(int)(TechData.Category.PSI)] = category_start_x[(int)(TechData.Category.PSI_BUY)] + (1 * SPACING);

        content_object.GetComponent<RectTransform>().sizeDelta = new Vector2((2 * HORIZONTAL_MARGIN) + ((num_cols - 1) * SPACING), (2 * VERTICAL_MARGIN) + ((num_rows - 1) * SPACING));

        // Parent and position each advance icon.
        foreach (AdvanceIcon cur_icon in advance_icon_objects)
        {
            cur_icon.gameObject.transform.SetParent(content_object.transform);
            cur_icon.gameObject.transform.localScale = new Vector3(ICON_INIT_SIZE, ICON_INIT_SIZE, ICON_INIT_SIZE);
            cur_icon.gameObject.GetComponent<RectTransform>().anchoredPosition = new Vector2(category_start_x[(int)(cur_icon.tech_data.category)] + (cur_icon.col * SPACING), VERTICAL_MARGIN + (cur_icon.row * SPACING));
        }

        // Create lines connecting advances with their prerequisites.
        foreach (AdvanceIcon cur_icon in advance_icon_objects)
        {
            bool purchasable = ((cur_icon.tech_data.category == TechData.Category.TECH_BUY) || (cur_icon.tech_data.category == TechData.Category.BIO_BUY) || (cur_icon.tech_data.category == TechData.Category.PSI_BUY) || (cur_icon.tech_data.category == TechData.Category.BASE_BUY));

            if (cur_icon.tech_data.prerequisite_tech_1 != -1) 
            {
                // Get the icon corresponding to this prereq advance.
                AdvanceIcon prereq_icon = AdvancesPanel.instance.advanceIconsHash[cur_icon.tech_data.prerequisite_tech_1];

                CreateConnectingLine(prereq_icon, cur_icon, purchasable);
            }

            if (cur_icon.tech_data.prerequisite_tech_2 != -1) 
            {
                // Get the icon corresponding to this prereq advance.
                AdvanceIcon prereq_icon = AdvancesPanel.instance.advanceIconsHash[cur_icon.tech_data.prerequisite_tech_2];

                CreateConnectingLine(prereq_icon, cur_icon, purchasable);
            }
        }

        // Create an AdvanceListLine for each advance (technology), other than random advances, and landscape object techs, which are hidden.
        foreach(KeyValuePair<int, TechData> entry in TechData.techs)
		{
            tech_data = entry.Value;

            // Skip technologies that do not fall into a display category.
            if (tech_data.category == TechData.Category.UNDEF) {
                continue;
            }

            // Instantiate an AdvanceListLine to represent this advance.
            GameObject new_line_object = Instantiate(advanceListLinePrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity);
            AdvanceListLine new_line = new_line_object.GetComponent<AdvanceListLine>();

            // Initialize the line for the current advance.
            new_line.Init(tech_data);

            // Add the new line to the list of AdvanceListLines
            advanceListLines.Add(new_line);

            // Add the new line to the hash of AdvanceListLines
            advanceListLinesHash.Add(tech_data.ID, new_line);

            // Add the new line to the list
            new_line_object.transform.SetParent(listContentObject.transform);
            new_line_object.transform.localScale = new Vector3(1, 1, 1);
        }
    }

    private void CreateConnectingLine(AdvanceIcon _low_icon, AdvanceIcon _high_icon, bool _purchasable)
    {
        AdvanceLine line_image = (AdvanceLine) Instantiate(_purchasable ? purchasableLinePrefab : advanceLinePrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity);
        line_image.gameObject.transform.SetParent(content_object.transform);
        line_image.gameObject.transform.SetAsFirstSibling();
        line_image.gameObject.transform.localScale = new Vector3(1, 1, 1);

        RectTransform imageRectTransform = line_image.gameObject.GetComponent<RectTransform>();

        Vector2 start_point = _low_icon.gameObject.GetComponent<RectTransform>().anchoredPosition + new Vector2(0f, 23f);
        Vector2 end_point = _high_icon.gameObject.GetComponent<RectTransform>().anchoredPosition + new Vector2(0f, -23f);

        Vector3 differenceVector = end_point - start_point;

        imageRectTransform.sizeDelta = new Vector2( differenceVector.magnitude, 3f);
        imageRectTransform.pivot = new Vector2(0, 0.5f);
        imageRectTransform.anchoredPosition = start_point;
        float angle = Mathf.Atan2(differenceVector.y, differenceVector.x) * Mathf.Rad2Deg;
        imageRectTransform.rotation = Quaternion.Euler(0,0, angle);
    }

    public void InfoEventReceived()
    {
        UpdateForAdvancePoints();
    }

    public void ResolutionChanged()
    {
        // Have the ZoomRect determine the min scale for the content image.
        zoomRect.DetermineMinScale();

        // Set the size of the description text to be smaller than the scroll rect it's in.
        advanceDescriptionText.gameObject.GetComponent<LayoutElement>().preferredWidth = (descriptionScrollRect.gameObject.GetComponent<RectTransform>().sizeDelta.x * 0.99f) - 20;
    }

    public void UpdateForLocalization()
    {
        foreach (AdvanceIcon cur_icon in advance_icon_objects) {
            cur_icon.UpdateForLocalization();
        }

        foreach (AdvanceListLine cur_line in advanceListLines) {
            cur_line.UpdateForLocalization();
        }
    }

    public void UpdateForAdvancePoints()
    {
        // Update the display of the currently selected advance.
        SelectAdvance(selectedAdvanceID, true);
    }

    public void UpdateForShow()
    {
        // Update the tree if it is shown, and requires an update.
        if ((mode == Mode.Tree) && treeRequiresUpdate) {
            UpdateTreeForTechnologies();
        }

        // Update the list if it is shown, and requires an update.
        if ((mode == Mode.List) && listRequiresUpdate) {
            UpdateListForTechnologies();
        }
    }

    public void UpdateForTechnologies()
    {
        if (gameObject.activeInHierarchy)
        {
            if (mode == Mode.Tree) {
                UpdateTreeForTechnologies();
            }

            if (mode == Mode.List) {
                UpdateListForTechnologies();
            }
        }
        else
        {
            // Both the tree and the list require an update when next shown.
            treeRequiresUpdate = true;
            listRequiresUpdate = true;
        }
    }

    public void UpdateTreeForTechnologies()
    {
        bool acquired, available;
        int num_available_for_research = 0;

        // By default, if there are no advances available to be researched, start with ID 0 being selected.
        advanceIDToSelect = 0;

        foreach (AdvanceIcon cur_icon in advance_icon_objects)
        {
            // Determine whether the technology represented by this icon has been acquired.
            acquired = (GameData.instance.GetTechCount(cur_icon.tech_ID) > 0);

            // Determine whether the technology represented by this icon is currently available to be acquired.
            available = ((!acquired) && RequirementsMet(cur_icon.tech_ID));

            // If in screenshot format, show all advances as acquired.
            if (screenshotFormat)
            {
                acquired = true;
                available = false;
            }

            cur_icon.UpdateForTechnology(acquired, available);

            // If this icon's tech is available to be researched...
            if (available && (cur_icon.tech_data.default_price == 0))
            {
                // Determine whether to select this icon's tech, such that each available tech ends up with an equal probability of being selected. 
                num_available_for_research++;
                if (UnityEngine.Random.value <= (1f / (float)num_available_for_research)) {
                    advanceIDToSelect = cur_icon.tech_ID;
                }
            }
        }

        if ((selectedAdvanceID == -1) && (advanceIDToSelect != -1)) {
            selectedAdvanceID = advanceIDToSelect;
        }

        // Refresh the info of the selected advance.
        SelectAdvance(selectedAdvanceID, true);

        // The tree has been updated.
        treeRequiresUpdate = false;
    }

    public void UpdateListForTechnologies()
    {
        // Update each line for the status of the advance it represents.
        foreach (AdvanceListLine cur_line in advanceListLines) {
            cur_line.UpdateStatus();
        }

        // Sort the list of AdvanceListLines
        advanceListLines.Sort(new AdvanceListLineComparer());

        // Start by hiding all title lines.
        titleAvailToReseach.SetActive(false);
        titleAvailToBuy.SetActive(false);
        titleUnavailToResearch.SetActive(false);
        titleUnavailToBuy.SetActive(false);
        titleResearched.SetActive(false);
        titleBought.SetActive(false);

        int index = 0;
        AdvanceListLine.Status prevStatus = AdvanceListLine.Status.UNDEF;
        foreach (AdvanceListLine cur_line in advanceListLines) 
        {
            // If we're starting into items with a different status, first activate and position the title line for this status.
            if (prevStatus != cur_line.status)
            {
                switch (cur_line.status)
                {
                    case AdvanceListLine.Status.AVAILABLE_TO_RESEARCH:
                        titleAvailToReseach.SetActive(true);
                        titleAvailToReseach.transform.SetSiblingIndex(index);
                        break;
                    case AdvanceListLine.Status.AVAILABLE_TO_BUY:
                        titleAvailToBuy.SetActive(true);
                        titleAvailToBuy.transform.SetSiblingIndex(index);
                        break;
                    case AdvanceListLine.Status.UNAVAILABLE_TO_RESEARCH:
                        titleUnavailToResearch.SetActive(true);
                        titleUnavailToResearch.transform.SetSiblingIndex(index);
                        break;
                    case AdvanceListLine.Status.UNAVAILABLE_TO_BUY:
                        titleUnavailToBuy.SetActive(true);
                        titleUnavailToBuy.transform.SetSiblingIndex(index);
                        break;
                    case AdvanceListLine.Status.RESEARCHED:
                        titleResearched.SetActive(true);
                        titleResearched.transform.SetSiblingIndex(index);
                        break;
                    case AdvanceListLine.Status.BOUGHT:
                        titleBought.SetActive(true);
                        titleBought.transform.SetSiblingIndex(index);
                        break;
                }

                prevStatus = cur_line.status;
                index++;
            }

            // Place the line for the current advance at this index.
            cur_line.gameObject.transform.SetSiblingIndex(index);
            cur_line.lineBackground.sprite = ((index % 2) == 0) ? darkLineSprite : lightLineSprite;
            index++;
        }

        // The list has been updated.
        listRequiresUpdate = false;
    }

    public void UpdateForTechPrices()
    {
        // Refresh the info of the selected advance.
        SelectAdvance(selectedAdvanceID, true);
    }

    public void UpdateForSetTarget()
    {
        // Clear the target status of all lines.
        foreach (AdvanceListLine cur_line in targetPreReqs) {
            cur_line.SetTargetStatus(AdvanceListLine.TargetStatus.NONE);
        }

        if (GameData.instance.targetAdvanceID != -1) {
            RecordTargetRequirements(GameData.instance.targetAdvanceID, AdvanceListLine.TargetStatus.TARGET);
        }
    }

    public void RecordTargetRequirements(int _advanceID, AdvanceListLine.TargetStatus _targetStatus)
    {
        // Get the AdvanceListLine representing the current target or prereq.
        AdvanceListLine cur_line = advanceListLinesHash[_advanceID];

        // Add the cur_line to the list of target prereq lines.
        targetPreReqs.Add(cur_line);

        //Debug.Log("RecordTargetRequirements() setting " + cur_line.techData.name + "'s target status to : " + _targetStatus);

        // Update the current line for its new target status.
        cur_line.SetTargetStatus(_targetStatus);

        // Call this method recursively for any of the current advance's pre-requisities.
        if (cur_line.techData.prerequisite_tech_1 != -1) RecordTargetRequirements(cur_line.techData.prerequisite_tech_1, AdvanceListLine.TargetStatus.PREREQ);
        if (cur_line.techData.prerequisite_tech_2 != -1) RecordTargetRequirements(cur_line.techData.prerequisite_tech_2, AdvanceListLine.TargetStatus.PREREQ);
    }

    public bool RequirementsMet(int _techID)
    {
        TechData techData = TechData.GetTechData(_techID);

        // Check whether we've met the prerequisite level.
        if (GameData.instance.level < techData.prerequisite_level) {
            return false;
        }

        // Check whether we have the first prerequisite technology.
        if ((techData.prerequisite_tech_1 != -1) && (GameData.instance.GetTechCount(techData.prerequisite_tech_1) == 0)) {
            return false;
        }

        // Check whether we have the second prerequisite technology.
        if ((techData.prerequisite_tech_2 != -1) && (GameData.instance.GetTechCount(techData.prerequisite_tech_2) == 0)) {
            return false;
        }

        // All requirements have been met.
        return true;
    }

    public void SelectAdvance(int _ID, bool _refresh)
    {
        // TESTING
        Debug.Log("SelectAdvance() _ID: " + _ID + ", _refresh: " + _refresh);

        bool selection_changed = (_ID != selectedAdvanceID);

        // Do nothing if this advance is already selected, and we're not refreshing it.
        if ((_ID == selectedAdvanceID) && (!_refresh)) {
            return;
        }

        // If there is a previously selected advance, shrink its icon.
        if ((selectedAdvanceID != -1) && selection_changed) 
        {
            TechData selected_tech = TechData.GetTechData(selectedAdvanceID);

            if (AdvancesPanel.instance.advanceIconsHash.ContainsKey(selectedAdvanceID))
            {
                // Get the icon corresponding to this advance.
                AdvanceIcon advance_icon = AdvancesPanel.instance.advanceIconsHash[selectedAdvanceID];
            
                // Shrink the icon of the previously selected advance, if it has one.
                if (advance_icon != null) {
                    ShrinkIcon();
                }
            }
        }

        // Record the newly selected advance.
        selectedAdvanceID = _ID;

        // If there is a newly selected advance...
        if ((selectedAdvanceID != -1) && selection_changed) 
        {
            TechData selected_tech = TechData.GetTechData(_ID);

            if (AdvancesPanel.instance.advanceIconsHash.ContainsKey(_ID))
            {
                // Get the icon corresponding to this advance.
                AdvanceIcon advance_icon = AdvancesPanel.instance.advanceIconsHash[_ID];

                //Debug.Log("SelectAdvance() called for " + _ID + ": " + selected_tech.name);

                // If the selected advance has an icon...
                if (advance_icon != null) 
                {
                    // Grow the icon of the newly selected advance
                    GrowIcon();

                    // If the selected advance is not within the visible area of the ScrollView, center it within the visible area.
                    Canvas.ForceUpdateCanvases();
                    RectTransform scrollRectRectTransform = scrollRect.gameObject.GetComponent<RectTransform>();
                    if (((advance_icon.gameObject.transform.localPosition.x * contentObjectRectTransform.localScale.x) < (-scrollRect.content.localPosition.x)) ||
                        ((advance_icon.gameObject.transform.localPosition.x * contentObjectRectTransform.localScale.x) > ((-scrollRect.content.localPosition.x) + scrollRectRectTransform.rect.width)) ||
                        (((advance_icon.gameObject.transform.localPosition.y * contentObjectRectTransform.localScale.y) + scrollRect.content.localPosition.y) > 0) ||
                        (((advance_icon.gameObject.transform.localPosition.y * contentObjectRectTransform.localScale.y) + scrollRect.content.localPosition.y) < (-(scrollRectRectTransform.rect.height))))
                    {
                        //Debug.Log("SelectAdvance() icon y: " + selected_tech.advance_icon.gameObject.transform.localPosition.y + ", content y: " + scrollRect.content.localPosition.y);
                        scrollRect.content.localPosition = -(advance_icon.gameObject.transform.localPosition * contentObjectRectTransform.localScale.x) + (new Vector3(scrollRectRectTransform.rect.width, -(scrollRectRectTransform.rect.height)) / 2);
                        zoomRect.ContentPositionChanged();
                    }
                }
            }
        }

        if (selectedAdvanceID != -1)
        {
            TechData selected_tech = TechData.GetTechData(_ID);

            // Get the tech's description text.
            string description_text = GetAdvanceDescriptionText(selected_tech, true);

            // Add the tech's name at the start of the desciption text.
            description_text = "<size=" + INFO_TEXT_LARGE + "><color=yellow>" + selected_tech.name + "</color></size>\n" + description_text;

            // Display the description text.
            advanceDescriptionText.text = description_text;

            if (AdvancesPanel.instance.advanceIconsHash.ContainsKey(_ID))
            {
                // Get the icon corresponding to this advance.
                AdvanceIcon advance_icon = AdvancesPanel.instance.advanceIconsHash[_ID];

                // Update research button
                if ((advance_icon == null) || (advance_icon.acquired))
                {
                    // If this advance is already acquired, do not show the research button at all.
                    button_area_object.SetActive(false);
                }
                else
                {
                    // This advance has not yet been acquired, so show the button.
                    button_area_object.SetActive(true);

                    if (selected_tech.default_price == 0)
                    {
                        // GB:Localization

                        string _research_button_text = LocalizationManager.GetTranslation("Advances Panel/research_button_text"); // "Research"
                        research_button_text.text = _research_button_text;
                        research_button_cost_text.text = "1";
                        credit_icon.SetActive(false);
                        credit_grayscale_icon.SetActive(false);

                        // Enable button (make interactable) only if the advance is available and there are enough advance points.
                        if (advance_icon.available && (GameData.instance.advance_points >= 1))
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
                        research_button_cost_text.text = string.Format("{0:n0}", GameData.instance.GetPrice(selected_tech.ID));
                        advance_point_icon.SetActive(false);
                        advance_point_grayscale_icon.SetActive(false);

                        // Enable button (make interactable) only if the advance is available (regardess of whether we have the credits; they can be bought).
                        if (advance_icon.available)
                        {
                            research_button.interactable = true;
                            credit_icon.SetActive(true);
                            credit_grayscale_icon.SetActive(false);
                        }
                        else 
                        {
                            research_button.interactable = false; 
                            credit_icon.SetActive(false);
                            credit_grayscale_icon.SetActive(true);
                        }
                    }
                }
            }
        }

        // Record that this user event has occurred
        GameData.instance.UserEventOccurred(GameData.UserEventType.SELECT_ADVANCE);
    }

    public string GetAdvanceNameText(TechData _advance_data)
    {
        return "<color=#" + ((_advance_data.category == TechData.Category.TECH) ? "7192ff" : ((_advance_data.category == TechData.Category.BIO) ? "ff4d4d" : ("be4dff"))) + ">" + _advance_data.name + "</color>";
    }

    public string GetAdvanceDescriptionText(TechData _advance_data, bool _compressed)
    {
        string description_text = "";

        // Reset the link count
        GameGUI.instance.linkManager.ResetLinks();

        // Determine the selected advance's time remaining.
        time_remaining_string = GameData.instance.ConstructTimeRemainingString(_advance_data.ID);

        // Get the advance's description summary text. If the advance has no desc, but it has a new_build, use the new_build's desc. 
        string description_summary = _advance_data.description;
        if (((description_summary == null) || (description_summary.Length == 0)) && (_advance_data.new_build != -1))
        {
            BuildData new_build_data = BuildData.GetBuildData(_advance_data.new_build);
            if (new_build_data != null) 
            {
                description_summary = new_build_data.description;
                description_summary += " <link=\"" + GameGUI.instance.linkManager.GetNumLinks() + "\"><u>(" + LocalizationManager.GetTranslation("Generic Text/more_word") + ")</u></link>";
                GameGUI.instance.linkManager.AddLink(LinkManager.LinkType.BUILD, _advance_data.new_build);
            }
        }

        description_text += "<size=" + INFO_TEXT_SMALL + ">" + description_summary + "</size>\n";

        if (!_compressed) description_text += "\n";

        // Add prerequisites
        if ((_advance_data.prerequisite_tech_1 != -1) || (_advance_data.prerequisite_tech_2 != -1) || (_advance_data.prerequisite_level > 0))
        {
            description_text += "<size=" + INFO_TEXT_MEDIUM + "><color=yellow>" + LocalizationManager.GetTranslation("Advances Panel/requires") + ":</color> ";

            if (_advance_data.prerequisite_tech_1 != -1) 
            {
                description_text += "<link=\"" + GameGUI.instance.linkManager.GetNumLinks() + "\"><u>" + TechData.GetTechData(_advance_data.prerequisite_tech_1).name + "</u></link>";
                GameGUI.instance.linkManager.AddLink(LinkManager.LinkType.TECH, _advance_data.prerequisite_tech_1);
            }

            if (_advance_data.prerequisite_tech_2 != -1) 
            {
                description_text += ", <link=\"" + GameGUI.instance.linkManager.GetNumLinks() + "\"><u>" + TechData.GetTechData(_advance_data.prerequisite_tech_2).name + "</u></link>";
                GameGUI.instance.linkManager.AddLink(LinkManager.LinkType.TECH, _advance_data.prerequisite_tech_2);
            }

            if (_advance_data.prerequisite_level > 0) 
            {
                if (_advance_data.prerequisite_tech_1 != -1) {
                    description_text += ", ";
                }
                description_text += LocalizationManager.GetTranslation("Advances Panel/level") + " " + _advance_data.prerequisite_level;
            }

            description_text += "</size>          ";
        }

        // Add duration
        if (_advance_data.duration_type == TechData.Duration.TEMPORARY)
        {
            description_text += "<size=" + INFO_TEXT_MEDIUM + "><color=yellow>" + LocalizationManager.GetTranslation("Generic Text/lasts_text") + ":</color> ";

            description_text += GameData.instance.GetDurationText(_advance_data.duration_time);

            if (String.Equals(time_remaining_string, "") == false)
            {
                description_text += " " + color_tag_inc + "(" + time_remaining_string + " " + LocalizationManager.GetTranslation("Generic Text/remaining_text") + ")</color>";
            }

            description_text += "</size>";
        }

        // If requirements or duration were given, add newline at end of line.
        if ((_advance_data.prerequisite_tech_1 != -1) || (_advance_data.prerequisite_tech_2 != -1) || (_advance_data.prerequisite_level > 0) || (_advance_data.duration_type == TechData.Duration.TEMPORARY)) {
            description_text += "\n";
        }
            
        // Add bonuses
        if ((_advance_data.bonus_type_1 != TechData.Bonus.UNDEF) || (_advance_data.bonus_type_2 != TechData.Bonus.UNDEF) || (_advance_data.bonus_type_3 != TechData.Bonus.UNDEF) || (_advance_data.new_build_name != ""))
        {
            Boolean bonus_added = false;
            description_text += "<size=" + INFO_TEXT_MEDIUM + "><color=yellow>" + LocalizationManager.GetTranslation("Advances Panel/bonuses") + ":</color> ";

            if (_advance_data.bonus_type_1 != TechData.Bonus.UNDEF) {
                description_text += GameGUI.instance.GetBonusText(_advance_data.bonus_type_1, _advance_data.GetBonusVal(1), _advance_data.GetBonusValMax(1), -1, true, GameGUI.instance.linkManager);
                bonus_added = true;
            }

            if (_advance_data.bonus_type_2 != TechData.Bonus.UNDEF) {
                description_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(_advance_data.bonus_type_2, _advance_data.GetBonusVal(2), _advance_data.GetBonusValMax(2), -1, true, GameGUI.instance.linkManager);
                bonus_added = true;
            }

            if (_advance_data.bonus_type_3 != TechData.Bonus.UNDEF) {
                description_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusText(_advance_data.bonus_type_3, _advance_data.GetBonusVal(3), _advance_data.GetBonusValMax(3), -1, true, GameGUI.instance.linkManager);
                bonus_added = true;
            }
                
            if (_advance_data.new_build_name != "")
            {
                description_text += (bonus_added ? ", " : "") + LocalizationManager.GetTranslation("Advances Panel/build") + " <link=\"" + GameGUI.instance.linkManager.GetNumLinks() + "\"><u>" + _advance_data.new_build_name + "</u></link>";
                GameGUI.instance.linkManager.AddLink(LinkManager.LinkType.BUILD, _advance_data.new_build);
            }

            if (_advance_data.new_object_name != "")
            {
                if (_advance_data.bonus_type_1 != TechData.Bonus.UNDEF) {
                    description_text += ", ";
                }

                description_text += LocalizationManager.GetTranslation("Advances Panel/exploit") + " <link=\"" + GameGUI.instance.linkManager.GetNumLinks() + "\"><u>" + _advance_data.new_object_name + "</u></link>";
                GameGUI.instance.linkManager.AddLink(LinkManager.LinkType.TECH, _advance_data.new_object);
            }
                
            description_text += "</size>\n";
        }

        return description_text;
    }

    public string GetAdvanceIconText(TechData _advance_data)
    {
        Boolean bonus_added = false;
        string description_text = "";

        // Add bonuses
        if ((_advance_data.bonus_type_1 != TechData.Bonus.UNDEF) || (_advance_data.bonus_type_2 != TechData.Bonus.UNDEF) || (_advance_data.bonus_type_3 != TechData.Bonus.UNDEF) || (_advance_data.new_build_name != ""))
        {
            if (_advance_data.bonus_type_1 != TechData.Bonus.UNDEF) {
                description_text += GameGUI.instance.GetBonusIconText(_advance_data.bonus_type_1, _advance_data.GetBonusVal(1), _advance_data.GetBonusValMax(1), -1, false, null);
                bonus_added = true;
            }

            if (_advance_data.bonus_type_2 != TechData.Bonus.UNDEF) {
                description_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusIconText(_advance_data.bonus_type_2, _advance_data.GetBonusVal(2), _advance_data.GetBonusValMax(2), -1, false, null);
                bonus_added = true;
            }

            if (_advance_data.bonus_type_3 != TechData.Bonus.UNDEF) {
                description_text += (bonus_added ? ", " : "") + GameGUI.instance.GetBonusIconText(_advance_data.bonus_type_3, _advance_data.GetBonusVal(3), _advance_data.GetBonusValMax(3), -1, false, null);
                bonus_added = true;
            }
                
            if (_advance_data.new_build_name != "")
            {
                description_text += (bonus_added ? ", " : "") + "<sprite=20>";
            }

            if (_advance_data.new_object_name != "")
            {
                description_text += (bonus_added ? ", " : "") + "<sprite=22>";
            }

            if (_advance_data.duration_type == TechData.Duration.TEMPORARY)
            {
                if (description_text.Length > 0) {
                    description_text += ", ";
                }

                description_text += "<sprite=29>";
            }

            return description_text;
        }

        return description_text;
    }

    public void GrowIcon()
    {
        if (isActiveAndEnabled) {
            StartCoroutine(GrowIcon_Coroutine());
        } else {
            // Set the selected advance's icon to its large scale.
            GameObject icon = AdvancesPanel.instance.advanceIconsHash[selectedAdvanceID].gameObject;
            icon.GetComponent<RectTransform>().localScale = new Vector3(ICON_GROW_SIZE, ICON_GROW_SIZE, ICON_GROW_SIZE);
        }
    }

    public IEnumerator GrowIcon_Coroutine()
    {
        // Get the icon corresponding to the selected advance.
        GameObject icon = AdvancesPanel.instance.advanceIconsHash[selectedAdvanceID].gameObject;

        // Have this icon appear on top of all others.
        icon.gameObject.transform.SetAsLastSibling();

        float start_time = Time.unscaledTime;
        float end_time = start_time + ICON_GROW_DURATION;

        // Interpolate scale over time.
        while (Time.unscaledTime <= end_time) 
        {
            icon.GetComponent<RectTransform>().localScale = Vector3.Slerp(new Vector3(ICON_INIT_SIZE, ICON_INIT_SIZE, ICON_INIT_SIZE), new Vector3(ICON_GROW_SIZE, ICON_GROW_SIZE, ICON_GROW_SIZE), Mathf.SmoothStep(0f, 1f, (Time.unscaledTime - start_time) / ICON_GROW_DURATION));
            yield return null;
        }

        // Set final scale
        icon.GetComponent<RectTransform>().localScale = new Vector3(ICON_GROW_SIZE, ICON_GROW_SIZE, ICON_GROW_SIZE);
    }

    public void ShrinkIcon()
    {
        if (isActiveAndEnabled) {
            StartCoroutine(ShrinkIcon_Coroutine());
        } else {
            // Set the selected advance's icon to its normal scale.
            GameObject icon = AdvancesPanel.instance.advanceIconsHash[selectedAdvanceID].gameObject;
            icon.GetComponent<RectTransform>().localScale = new Vector3(ICON_INIT_SIZE, ICON_INIT_SIZE, ICON_INIT_SIZE);
        }
    }

    public IEnumerator ShrinkIcon_Coroutine()
    {
        // Get the icon corresponding to the selected advance.
        GameObject icon = AdvancesPanel.instance.advanceIconsHash[selectedAdvanceID].gameObject;

        float start_time = Time.unscaledTime;
        float end_time = start_time + ICON_SHRINK_DURATION;

        // Interpolate scale over time.
        while (Time.unscaledTime <= end_time) 
        {
            icon.GetComponent<RectTransform>().localScale = Vector3.Slerp(new Vector3(ICON_GROW_SIZE, ICON_GROW_SIZE, ICON_GROW_SIZE), new Vector3(ICON_INIT_SIZE, ICON_INIT_SIZE, ICON_INIT_SIZE), Mathf.SmoothStep(0f, 1f, (Time.unscaledTime - start_time) / ICON_SHRINK_DURATION));
            yield return null;
        }

        // Set final scale
        icon.GetComponent<RectTransform>().localScale = new Vector3(ICON_INIT_SIZE, ICON_INIT_SIZE, ICON_INIT_SIZE);
    }

    public void SetScreenshotFormat(bool _screenshotFormat)
    {
        screenshotFormat = _screenshotFormat;

        if (screenshotFormat)
        {
            advancePanelRectTransform.anchorMin = new Vector2(0.1f, -0.34f);
            advancePanelRectTransform.anchorMax = new Vector2(0.81f, 1.18f);
            //contentBGImage.enabled = true;

            // Deselect selected advance
            SelectAdvance(-1, true);
        }
        else
        {
            advancePanelRectTransform.anchorMin = new Vector2(0.4f, 0.0f);
            advancePanelRectTransform.anchorMax = new Vector2(0.6f, 1.0f);
            //contentBGImage.enabled = false;
        }

        UpdateForTechnologies();
    }

    public void OnClick_ModeButton()
    {
        SetMode((mode == Mode.Tree) ? Mode.List : Mode.Tree);
    }

    public void OnClick_ResearchSelectedAdvance()
    {
        TechData tech_data = TechData.GetTechData(selectedAdvanceID);

        if (tech_data == null) {
            return;
        }

        if (tech_data.default_price == 0)
        {
            // Send message to server requesting research of this advance.
            Network.instance.SendCommand("action=research_advance|techID=" + selectedAdvanceID);

            // Tell the tutorial system that this research command was sent.
            Tutorial.instance.ResearchCommandSent();

            // Tell the GUI that an advance has just been researched, so that it can display rating request if appropriate.
            GameGUI.instance.OnAdvanceResearched();
        }
        else
        {
            if (GameData.instance.GetPrice(tech_data.ID) > GameData.instance.credits)
            {
                GameGUI.instance.RequestBuyCredits();
            }
            else 
            {
                // Send message to server requesting purchase of this advance.
                Network.instance.SendCommand("action=purchase_advance|techID=" + selectedAdvanceID);

                // Send analytics custom event
                Analytics.CustomEvent("purchaseAdvance", new Dictionary<string, object>
                {
                    { "advanceName", tech_data.name },
                    { "advanceID", tech_data.ID },
                    { "nationName", GameData.instance.nationName },
                    { "nationID", GameData.instance.nationID },
                    { "nationLevel", GameData.instance.level }
                });
            }
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
                SelectAdvance(GameGUI.instance.linkManager.link_ids[link_index], true);
            }
            else if (GameGUI.instance.linkManager.link_types[link_index] == LinkManager.LinkType.STAT)
            {
                StatDetailsPanel.instance.ActivateForBonus((TechData.Bonus)(GameGUI.instance.linkManager.link_ids[link_index]));
            }
        }
    }
}
