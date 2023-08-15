using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class MapRect : MonoBehaviour
{
    public ZoomRect zoomRect;
    public Image boundaryWest, boundaryEast, boundaryVeteran;

    List<MapMarker> map_markers = new List<MapMarker>();

    public void Start()
    {
        zoomRect.OnResize += Layout;
    }
 
    public void OnDestroy()
    {
        zoomRect.OnResize -= Layout;
    }

    public MapMarker AddMarker(int _blockX, int _blockZ, MapMarker.Type _type,  string _text, float _map_margin)
    {
        GameObject marker_object = MemManager.instance.GetMapMarkerObject();
        MapMarker map_marker = marker_object.GetComponent<MapMarker>();

        // Add the new MapMarker to the list.
        map_markers.Add(map_marker);

        marker_object.GetComponent<RectTransform>().localScale = new Vector3(1, 1, 1);
        marker_object.transform.SetParent(zoomRect.content);

        // Show flags in front of other kinds of markers.
        if (_type == MapMarker.Type.Flag) {
            marker_object.transform.SetAsLastSibling();
        } else {
            marker_object.transform.SetAsFirstSibling();
        }

        // Initialize the new MapMarker
        map_marker.Init(_blockX, _blockZ, _type, _text, _map_margin);

        // Set position and layout of map marker based on content and map rect size.
        map_marker.Layout(zoomRect.GetContentRefSize().x, zoomRect.content.rect.width, zoomRect.content.rect.height, zoomRect.zoomRectTransform.rect.width);

        return map_marker;
    }

    public void Layout()
    {
        foreach (MapMarker marker in map_markers) {
            marker.Layout(zoomRect.GetContentRefSize().x, zoomRect.content.rect.width, zoomRect.content.rect.height, zoomRect.zoomRectTransform.rect.width);
        }

        // Layout boundary lines
        LayoutVerticalBoundaryLine(boundaryWest, GameData.instance.map_position_limit);
        LayoutVerticalBoundaryLine(boundaryEast, GameData.instance.map_position_eastern_limit);
        LayoutHorizontalBoundaryLine(boundaryVeteran, GameData.instance.newPlayerAreaBoundary);
    }

    public void LayoutVerticalBoundaryLine(Image _boundaryLine, int _blockX)
    {
        if ((_blockX < 0) || (_blockX >= MapView.instance.mapDimX))
        {
            // The boundary line is off the map; hide it.
            _boundaryLine.gameObject.SetActive(false);
        }
        else
        {
            // Show the boundary line.
            _boundaryLine.gameObject.SetActive(true);
            _boundaryLine.gameObject.transform.SetAsFirstSibling();

            float content_ref_width = zoomRect.GetContentRefSize().x;
            float content_ref_height = zoomRect.GetContentRefSize().y;
            float content_width = zoomRect.content.rect.width;

            float world_ref_scale = (content_ref_width - (2 * MapPanel.MAP_MARGIN)) / MapView.instance.mapDimX;
            float ref_map_scale = (content_width / content_ref_width);
            _boundaryLine.gameObject.GetComponent<RectTransform>().localPosition = new Vector3((MapPanel.MAP_MARGIN + (_blockX * world_ref_scale)) * ref_map_scale, -MapPanel.MAP_MARGIN * ref_map_scale, 0);
            _boundaryLine.gameObject.GetComponent<RectTransform>().localScale = new Vector3(ref_map_scale, ref_map_scale, 1);
            _boundaryLine.gameObject.GetComponent<RectTransform>().sizeDelta = new Vector2(4, content_ref_height - (2 * MapPanel.MAP_MARGIN));
        }
    }

    public void LayoutHorizontalBoundaryLine(Image _boundaryLine, int _blockZ)
    {
        if ((_blockZ < 0) || (_blockZ >= MapView.instance.mapDimZ))
        {
            // The boundary line is off the map; hide it.
            _boundaryLine.gameObject.SetActive(false);
        }
        else
        {
            // Show the boundary line.
            _boundaryLine.gameObject.SetActive(true);
            _boundaryLine.gameObject.transform.SetAsFirstSibling();

            float content_ref_width = zoomRect.GetContentRefSize().x;
            float content_ref_height = zoomRect.GetContentRefSize().y;
            float content_width = zoomRect.content.rect.width;

            float world_ref_scale = (content_ref_width - (2 * MapPanel.MAP_MARGIN)) / MapView.instance.mapDimX;
            float ref_map_scale = (content_width / content_ref_width);
            _boundaryLine.gameObject.GetComponent<RectTransform>().localPosition = new Vector3(MapPanel.MAP_MARGIN * ref_map_scale, -(MapPanel.MAP_MARGIN + (_blockZ * world_ref_scale)) * ref_map_scale, 0);
            _boundaryLine.gameObject.GetComponent<RectTransform>().localScale = new Vector3(ref_map_scale, ref_map_scale, 1);
            _boundaryLine.gameObject.GetComponent<RectTransform>().sizeDelta = new Vector2(4, content_ref_width - (2 * MapPanel.MAP_MARGIN));
        }
    }

    public void RemoveMarkersOfType(MapMarker.Type _type)
    {
        for (int i = map_markers.Count - 1; i > -1; i--)
        {
            if (map_markers[i].type == _type)
            {
                // Remove the current map marker from the map.
                map_markers[i].gameObject.transform.SetParent(null);
                MemManager.instance.ReleaseMapMarkerObject(map_markers[i].gameObject);

                // Remove the current map marker from the list.
                map_markers.RemoveAt(i);
            }
        }
    }

    public void RemoveMarkersAtLocation(int _x, int _z, MapMarker.Type _type)
    {
        for (int i = map_markers.Count - 1; i > -1; i--)
        {
            if ((map_markers[i].blockX == _x) && (map_markers[i].blockZ == _z) && (map_markers[i].type == _type))
            {
                // Remove the current map marker from the map.
                map_markers[i].gameObject.transform.SetParent(null);
                MemManager.instance.ReleaseMapMarkerObject(map_markers[i].gameObject);

                // Remove the current map marker from the list.
                map_markers.RemoveAt(i);
            }
        }
    }
}
