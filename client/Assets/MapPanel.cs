using UnityEngine;
using UnityEngine.UI;
using System.IO;
using System.Collections;

public class MapPanel : MonoBehaviour
{
    public enum Mode
    {
        Map,
        List
    }

    public static MapPanel instance;

    public MapRect mapRect;
    public ZoomRect zoomRect;
    public GameObject mapContentObject, mapContentArea, listContentArea, listObject, pageControls;
    public RectTransform scrollRectTransform, mapContentRectTransform;
    public Image modeButtonImage;
    public Sprite modeMapSprite, modeListSprite;
    public Sprite lineDark, lineLight;
    public TMPro.TextMeshProUGUI pageText;
    public Mode mode = Mode.Map;

    int mapImageWidth, mapImageHeight;
    float prevRequestNationAreasTime = -1000000;
    MapMarker mapViewMarker = null;
    bool instantTransitionUponClose, transitionedMapView;
    int startIndex = 0;
    public const float MAP_MARGIN = 160;
    const float MAP_INIT_RESIZE_DURATION = 1f;
    const float REQUEST_NATION_AREAS_PERIOD = 180f;
    const int PAGE_SIZE = 100;

	public MapPanel()
    {
        instance = this;
	}

    public void Start()
    {
        SetMode(mode);
    }

    public void OnEnable()
    {
        if ((GameData.instance == null) || (GameData.instance.userID == -1)) {
            return;
        }

        /*
        // TESTING
        for (int i = 0; i < 5; i++)
        {
            mapRect.AddMarker(Random.Range(0, MapView.instance.mapDimX), Random.Range(0, MapView.instance.mapDimZ), MapMarker.Type.Flag, "Here's a map flag, test #" + i, MAP_MARGIN);
        }
        */

        // Set layout for current resolution
        ResolutionChanged();
       
        // Add or update the new map view marker (this way, it remains sandwiched between the nation area and flag markers.)
        if (mapViewMarker == null) {
            mapViewMarker = mapRect.AddMarker(MapView.instance.GetViewBlockX(), MapView.instance.GetViewBlockZ(), MapMarker.Type.MapView, "", MAP_MARGIN);
        } else {
            mapViewMarker.Init(MapView.instance.GetViewBlockX(), MapView.instance.GetViewBlockZ(), MapMarker.Type.MapView, "", MAP_MARGIN);
        }

        // Request the list of the nation's areas, if appropriate.
        if ((Time.unscaledTime - prevRequestNationAreasTime) >= REQUEST_NATION_AREAS_PERIOD)
        {
            Network.instance.SendCommand("action=request_nation_areas");
            prevRequestNationAreasTime = Time.unscaledTime;
        }

        // If showing list of map flags, init each line for its sort order.
        if (mode == Mode.List) {
            InitLinesForSort();
        }

        if (mode == Mode.Map)
        {
            StartCoroutine(TransitionIn());
            transitionedMapView = true;
        }
        else
        {
            transitionedMapView = false;
        }

        // Record that the map should not be instantly transitioned back upon the closing of this window.
        instantTransitionUponClose = false;
    }

    public void SetMode(Mode _mode)
    {
        // Record the new mode.
        mode = _mode;

        // Activate the appropriate content area.
        mapContentArea.SetActive(mode == Mode.Map);
        listContentArea.SetActive(mode == Mode.List);

        // Set the image on the mode button to represent the opposite mode.
        modeButtonImage.sprite = (mode == Mode.Map) ? modeListSprite : modeMapSprite;

        // If showing list of map flags, init each line for its sort order.
        if (mode == Mode.List) {
            InitLinesForSort();
        }
    }

    public IEnumerator TransitionIn()
    {
        // Update the UI layout so that its numbers are correct before getting to the below code.
        Canvas.ForceUpdateCanvases();

        // Have the map zoom rect determine the min scale for the content image.
        zoomRect.DetermineMinScale();

        // Determine the map's upper left and upper right points in screen space.
        Vector2 ulScreenPoint = new Vector2(GameGUI.instance.GetMainUILeftWidth(), Screen.height);
        Vector2 urScreenPoint = new Vector2(Screen.width, Screen.height);

        // Determine the map's upper left and upper right points in world space.
        Vector3 ulWorldPoint = MapView.instance.GetWorldPoint(ulScreenPoint);
        Vector3 urWorldPoint = MapView.instance.GetWorldPoint(urScreenPoint);
        //Debug.Log("ulWorldPoint: " + ulWorldPoint + ", urWorldPoint: " + urWorldPoint);

        // Determine the world space to screen space scale.
        float screenMapScale = (urScreenPoint.x - ulScreenPoint.x) / (urWorldPoint.x - ulWorldPoint.x);

        // Determine the map scroll rect's position in screen space.
        Rect rect = GameGUI.RectTransformToScreenSpace(scrollRectTransform);
        //Debug.Log("Rect: " + rect.x + "," + rect.y + "  dims: " + rect.width + "," + rect.height);

        // Determine the map scroll rect's uppr left point in world space.
        Vector3 scrollRectULWorldPoint = ulWorldPoint + new Vector3((rect.x - ulScreenPoint.x) / screenMapScale, 0, -rect.y / screenMapScale);

        //Debug.Log("ulWorldPoint.x: " + ulWorldPoint.x + ", rect.x: " + rect.x + ", ulScreenPoint.x: " + ulScreenPoint.x + ", screenMapScale: " + screenMapScale + ", scrollRectULWorldPoint.x: " + scrollRectULWorldPoint.x);
        //Debug.Log("ulWorldPoint.z: " + ulWorldPoint.z + ", rect.y: " + rect.y + ", screenMapScale: " + screenMapScale + ", scrollRectULWorldPoint.z: " + scrollRectULWorldPoint.z);

        // Determine the size that the UI map image should be set to, to match the world view on the screen.
        float uiMapWidth0 = MapView.instance.mapDimX * MapView.BLOCK_SIZE * screenMapScale / MapView.instance.canvas.scaleFactor;
        float uiMapHeight0 = MapView.instance.mapDimZ * MapView.BLOCK_SIZE * screenMapScale / MapView.instance.canvas.scaleFactor;
        float uiMapImageScale = uiMapWidth0 / (mapImageWidth - (2 * MAP_MARGIN));
        //Debug.Log("mapDimX * block size: " + MapView.instance.mapDimX * MapView.BLOCK_SIZE + ", screenMapScale: " + screenMapScale + ", MapView.instance.canvas.scaleFactor: " + MapView.instance.canvas.scaleFactor + ", uiMapWidth: " + uiMapWidth);
        //Debug.Log("uiMapImageScale (" + uiMapImageScale + ") = uiMapWidth (" + uiMapWidth + ") / (mapImageWidth - (2 * MAP_MARGIN)) (" + (mapImageWidth - (2 * MAP_MARGIN)) + ")");

        // Add the margin dimensions to the ui map size
        uiMapWidth0 += (2 * MAP_MARGIN * uiMapImageScale);
        uiMapHeight0 += (2 * MAP_MARGIN * uiMapImageScale);

        // Set the UI map image to the determined size.
        mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, uiMapWidth0);
        mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, uiMapHeight0);

        // Position the UI map image so that it lines up with the view of the map.
        Vector3 contentPos = new Vector3(-((scrollRectULWorldPoint.x / MapView.BLOCK_SIZE / MapView.instance.mapDimX * (mapImageWidth - (2 * MAP_MARGIN)) + MAP_MARGIN) * uiMapImageScale), ((-scrollRectULWorldPoint.z / MapView.BLOCK_SIZE / MapView.instance.mapDimZ * (mapImageHeight - (2 * MAP_MARGIN)) + MAP_MARGIN) * uiMapImageScale), 0);
        //Debug.Log("content pos: " + contentPos);
        //Debug.Log("-scrollRectULWorldPoint.z / block_size: " + (-scrollRectULWorldPoint.z / MapView.BLOCK_SIZE) + " / mapDimZ (" + MapView.instance.mapDimZ + "): " + (-scrollRectULWorldPoint.z / MapView.BLOCK_SIZE / MapView.instance.mapDimZ) + ", mapImageHeight: " + mapImageHeight + " into map image w/ margins: " + (-scrollRectULWorldPoint.z / MapView.BLOCK_SIZE / MapView.instance.mapDimZ * (mapImageHeight - (2 * MAP_MARGIN)) + MAP_MARGIN) + ", uiMapImageScale: " + uiMapImageScale);
        mapContentRectTransform.localPosition = contentPos;

        // Determine size to resize ui map to.
        float uiMapWidth1 = Mathf.Min(uiMapWidth0, scrollRectTransform.sizeDelta.x * 3);
        float uiMapHeight1 = uiMapWidth1 / uiMapWidth0 * uiMapHeight0;

        // Determine position within map image that is the focus at the center of the view.
        float mapFocusX = ((-contentPos.x) + (scrollRectTransform.sizeDelta.x / 2)) / uiMapWidth0;
        float mapFocusY = (contentPos.y + (scrollRectTransform.sizeDelta.y / 2)) / uiMapHeight0;
        //Debug.Log("contentPos.y: " + contentPos.y + ", scrollRectTransform.sizeDelta.y: " + scrollRectTransform.sizeDelta.y + ", uiMapHeight0: " + uiMapHeight0);
        //Debug.Log("mapFocusX: " + mapFocusX + ", mapFocusY: " + mapFocusY);

        // Update layout of all map markers.
        mapRect.Layout();

        float cur_width, cur_height;
        float progress, cur_time, start_time = Time.unscaledTime;
        while ((cur_time = Time.unscaledTime - start_time) < MAP_INIT_RESIZE_DURATION)
        {
            progress = cur_time / MAP_INIT_RESIZE_DURATION;
            progress = Mathf.Pow(progress, .6f);
            cur_width = uiMapWidth0 + (progress * (uiMapWidth1 - uiMapWidth0));
            cur_height = uiMapHeight0 + (progress * (uiMapHeight1 - uiMapHeight0));
            mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, cur_width);
            mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, cur_height);
            mapContentRectTransform.localPosition = new Vector3(Mathf.Min(0, Mathf.Max(scrollRectTransform.sizeDelta.x - cur_width, -((mapFocusX * cur_width) - (scrollRectTransform.sizeDelta.x / 2)))), Mathf.Max(0, Mathf.Min(cur_height - scrollRectTransform.sizeDelta.y, (mapFocusY * cur_height) - (scrollRectTransform.sizeDelta.y / 2))), 0);

            // Update layout of all map markers.
            mapRect.Layout();

            yield return null;
        }
        
        mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Horizontal, uiMapWidth1);
        mapContentRectTransform.SetSizeWithCurrentAnchors(RectTransform.Axis.Vertical, uiMapHeight1);
        mapContentRectTransform.localPosition = new Vector3(Mathf.Min(0, Mathf.Max(scrollRectTransform.sizeDelta.x - uiMapWidth1, -((mapFocusX * uiMapWidth1) - (scrollRectTransform.sizeDelta.x / 2)))), Mathf.Max(0, Mathf.Min(uiMapHeight1 - scrollRectTransform.sizeDelta.y, (mapFocusY * uiMapHeight1) - (scrollRectTransform.sizeDelta.y / 2))), 0);

        // Update layout of all map markers.
        mapRect.Layout();

        //Debug.Log("mapFocusX: " + mapFocusX + ", uiMapWidth1: " + uiMapWidth1 + ", scrollRectTransform.sizeDelta.x: " + scrollRectTransform.sizeDelta.x);
        //Debug.Log("mapFocusY: " + mapFocusY + ", uiMapHeight1: " + uiMapHeight1 + ", scrollRectTransform.sizeDelta.y: " + scrollRectTransform.sizeDelta.y);
    }

    public void TransitionInstantlyUponClose()
    {
        instantTransitionUponClose = true;
    }

    public void ClosingPanel()
    {
        if (transitionedMapView) {
            MapView.instance.ExitMapPanelView(instantTransitionUponClose ? 0f : GameGUI.MAP_PANEL_TRANSITION_DURATION);
        }
    }

    public void ResolutionChanged()
    {
        if (gameObject.activeInHierarchy) 
        {
            // Have the map ZoomRect determine the minimum size that the map image can be scaled down to, and still fill the rect.
            zoomRect.DetermineMinScale();
        }
    }

    public bool LoadUIMapImage(int _serverID)
    {
        string filePath = Application.persistentDataPath + "/ui_map_" + _serverID + ".jpg"; 

        if (File.Exists(filePath) == false) {
            return false;
        }
         
        // Attempt to load the map image.
        byte[] fileData = File.ReadAllBytes(filePath);
        Texture2D tex = new Texture2D(2, 2);
        tex.filterMode = FilterMode.Trilinear;
        bool result = tex.LoadImage(fileData); //..this will auto-resize the texture dimensions.

        if (result == false) {
            return false;
        }

        // Record the dimensions of the map image.
        mapImageWidth = tex.width;
        mapImageHeight = tex.height;

        // Create a sprite for the map image
        Sprite sprite = Sprite.Create(tex, new Rect(0,0,mapImageWidth, mapImageHeight), new Vector2(0.0f,0.0f), 100.0f);
        mapContentObject.GetComponent<UnityEngine.UI.Image> ().sprite = sprite;

        // Set the initial size of the map object to the size of the map image itself.
        mapContentRectTransform.sizeDelta = new Vector2(mapImageWidth, mapImageHeight);

        // Set the map ZoomRect's reference size for the content
        zoomRect.SetContentRefSize(new Vector2(mapImageWidth, mapImageHeight));

        return true;
    }

    public void NationAreasReceived()
    {
        // Remove all old nation area markers.
        mapRect.RemoveMarkersOfType(MapMarker.Type.NationArea);

        // Add the new nation area markers
        foreach (NationArea cur_area in GameData.instance.nationAreas) {
            Debug.Log("nation area at: " + cur_area.x + "," + cur_area.y);
            mapRect.AddMarker(cur_area.x, cur_area.y, MapMarker.Type.NationArea, "", MAP_MARGIN);
        }
    }

    public void MapFlagsReceived()
    {
        // Reset the startIndex to 0.
        startIndex = 0;

        // Sort the list of MapFlagRecords
        GameData.instance.mapFlags.Sort(new MapFlagRecordComparer());

        // Clear all map flags from Map Panel.
        ClearMapFlags();

        // Add the current page of flags to both the map rect and the flag list.
        ShowMapFlagsPage();

        // Update the page controls.
        UpdatePageControls();
    }

    public void MapFlagAdded(MapFlagRecord _mapFlagRecord, int _index)
    {
        // If the added flag is on the page being shown, display it.
        if ((_index >= startIndex) && (_index < (startIndex + PAGE_SIZE))) {
            MapPanel.instance.SetMapFlag(_mapFlagRecord.x, _mapFlagRecord.z, _mapFlagRecord.text, _index - startIndex, true);
        }

        // Update the page controls.
        UpdatePageControls();
    }

    public string GetMapFlagText(int _x, int _z)
    {
        foreach(Transform child in listObject.transform) 
        {
            MapFlagLine line = child.gameObject.GetComponent<MapFlagLine>();
            if ((line.x == _x) && (line.z == _z))
            {
                return line.title;
                break;
            }
        }

        return "";
    }

    public void SetMapFlag(int _x, int _z, string _text, int _index, bool _delete_first)
    {
        if (_delete_first)
        {
            // Remove any flag marker and flag line for a flag already at this location.
            DeleteMapFlag(_x, _z);
        }

        // Add a marker for the given map flag.
        mapRect.AddMarker(_x, _z, MapMarker.Type.Flag, _text, MAP_MARGIN);

        // Add a line to the map flags list
        MapFlagLine mapFlagLine = MemManager.instance.GetMapFlagLineObject().GetComponent<MapFlagLine>();
        mapFlagLine.gameObject.transform.SetParent(listObject.transform);
        mapFlagLine.gameObject.transform.localScale = new Vector3(1, 1, 1);
        mapFlagLine.Init(_x, _z, _text);

        // Set this flag's position in the list
        mapFlagLine.gameObject.transform.SetSiblingIndex(_index);

        //Debug.Log("Created flag with text: " + _text + ", title: " + mapFlagLine.title); // TESTING
    }

    public void DeleteMapFlag(int _x, int _z)
    {
        // Remove any flag marker already at this location.
        mapRect.RemoveMarkersAtLocation(_x, _z, MapMarker.Type.Flag);

        // Remove the line corresponding to this map flag from the map flag list.
        foreach(Transform child in listObject.transform) 
        {
            MapFlagLine line = child.gameObject.GetComponent<MapFlagLine>();
            if ((line.x == _x) && (line.z == _z))
            {
                line.gameObject.transform.SetParent(null);
                MemManager.instance.ReleaseMapFlagLineObject(line.gameObject);
                break;
            }
        }

        // If the list is shown, init each line for its new sort order.
        if (listContentArea.activeInHierarchy) {
            InitLinesForSort();
        }
    }

    public void ClearMapFlags()
    {
        // Remove all old map flag markers.
        mapRect.RemoveMarkersOfType(MapMarker.Type.Flag);

        // Reset prevRequestNationAreasTime so that nation areas will be fetched.
        prevRequestNationAreasTime = -1000000;

        // Clear all lines from the map flags list
        while (listObject.transform.childCount > 0)
        {
            GameObject removed = listObject.transform.GetChild(0).gameObject;
            removed.transform.SetParent(null);
            MemManager.instance.ReleaseMapFlagLineObject(removed);
        }
    }

    public void ShowMapFlagsPage()
    {
        MapFlagRecord curRecord;
        int endIndex = Mathf.Min(GameData.instance.mapFlags.Count - 1, startIndex + PAGE_SIZE - 1);
        for (int i = startIndex; i <= endIndex; i++)
        {
            // Add this map flag to the map panel.
            curRecord = GameData.instance.mapFlags[i];
            MapPanel.instance.SetMapFlag(curRecord.x, curRecord.z, curRecord.text, i - startIndex, false);
        }

        // If the list of map flags is shown, init the lines for their sort order.
        if (mode == Mode.List) {
            InitLinesForSort();
        }
    }
    /*
    public void SortMapFlagsList()
    {
        // Do not sort if there are fewer than 2 lines.
        if (listObject.transform.childCount < 2) {
            return;
        }

        bool changeMade;

        do {
            changeMade = false;

            MapFlagLine cur, next = listObject.transform.GetChild(0).GetComponent<MapFlagLine>();
            for (int i = 0; i < (listObject.transform.childCount - 1); i++) {
                cur = next;
                next = listObject.transform.GetChild(i + 1).GetComponent<MapFlagLine>();

                if (((sortMode == SortMode.Title) && (string.Compare(cur.title, next.title) == 1)) ||
                    ((sortMode == SortMode.Location && (cur.x > next.x)))) 
                {
                    // Reverse the order of the cur and next lines.
                    next.gameObject.transform.SetSiblingIndex(i);
                    next = cur;
                    changeMade = true;
                }
            }
        } while (changeMade);

        // If the list is shown, init each line for its new sort order.
        if (listContentArea.activeInHierarchy) {
            InitLinesForSort();
        }
    }
    */

    public void InitLinesForSort()
    {
        // Initialize each map flag line for the new sort order.
        foreach(Transform child in listObject.transform) {
            child.gameObject.GetComponent<MapFlagLine>().InitForSort();
        }
    }

    public void OnClick_ModeButton()
    {
        SetMode((mode == Mode.Map) ? Mode.List : Mode.Map);
    }

    public void OnClick_SortModeLocation()
    {
        if (MapFlagRecord.sortMode != MapFlagRecord.SortMode.Location)
        {
            MapFlagRecord.sortMode = MapFlagRecord.SortMode.Location;

            // Sort the list of MapFlagRecords
            GameData.instance.mapFlags.Sort(new MapFlagRecordComparer());

            // Clear all map flags from Map Panel.
            ClearMapFlags();

            // Add the current page of flags to both the map rect and the flag list.
            ShowMapFlagsPage();
        }
    }

    public void OnClick_SortModeTitle()
    {
        if (MapFlagRecord.sortMode != MapFlagRecord.SortMode.Text)
        {
            MapFlagRecord.sortMode = MapFlagRecord.SortMode.Text;

            // Sort the list of MapFlagRecords
            GameData.instance.mapFlags.Sort(new MapFlagRecordComparer());

            // Clear all map flags from Map Panel.
            ClearMapFlags();

            // Add the current page of flags to both the map rect and the flag list.
            ShowMapFlagsPage();
        }
    }

    public void OnClick_Flag(int _x, int _z)
    {
        // If we're not looking at the mainland map, do nothing.
        if (GameData.instance.mapMode != GameData.MapMode.MAINLAND) {
            return;
        }

        // Tell the map panel to transition the map camera back instantly upon closing, so its orientation will be correct when the new map data arrives.
        MapPanel.instance.TransitionInstantlyUponClose();

        // Close the map panel.
        GameGUI.instance.CloseActiveGamePanel();

        // Send message to server, to set view to the map location of this marker.
        Network.instance.SendCommand("action=event_center_on_block|blockX=" + _x + "|blockY=" + _z, true);
    }

    public void OnClick_PageDown()
    {
        if (startIndex > 0)
        {
            // Decrement page
            startIndex -= PAGE_SIZE;

            // Clear all map flags from Map Panel.
            ClearMapFlags();

            // Add the current page of flags to both the map rect and the flag list.
            ShowMapFlagsPage();

            // Update the page controls.
            UpdatePageControls();
        }
    }

    public void OnClick_PageUp()
    {
        if ((startIndex + PAGE_SIZE) < GameData.instance.mapFlags.Count)
        {
            // Increment page
            startIndex += PAGE_SIZE;

            // Clear all map flags from Map Panel.
            ClearMapFlags();

            // Add the current page of flags to both the map rect and the flag list.
            ShowMapFlagsPage();

            // Update the page controls.
            UpdatePageControls();
        }
    }

    public void UpdatePageControls()
    {
        // Show the page controls if there is more than one page of flags.
        pageControls.SetActive(GameData.instance.mapFlags.Count > PAGE_SIZE);

        // Display the current page number, and the total number of pages.
        pageText.text = ((startIndex / PAGE_SIZE) + 1) + "/" + (int)Mathf.Ceil((float)GameData.instance.mapFlags.Count / (float)PAGE_SIZE);
    }
}
