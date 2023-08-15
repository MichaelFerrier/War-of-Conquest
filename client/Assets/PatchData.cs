using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class PatchData
{
    public Terrain terrain;

    public Stack<GameObject> audioSources = new Stack<GameObject>();

    public bool pendingData = true;
    public bool pendingUpdate = true;
    public bool insideMap = true;
    public int numBlocksInView = 0;

    public void Init(bool _inside_map)
    {
        insideMap = _inside_map;
        pendingData = _inside_map; // Chunks outside of the map are not pending data; data is only received for chunks in the map.
        pendingUpdate = true;
        numBlocksInView = 0;

        // Initialize the terrain object to being inactive, so it won't use any rendering resources.
        terrain.gameObject.SetActive(!_inside_map);

        // Clear the list of object AudioSources, destroying each object.
        while (audioSources.Count > 0) {
            GameObject.Destroy(audioSources.Pop());
        }
    }

    public void IncrementNumBlocksInView()
    {
        // Increment the number of this patch's blocks that are in view.
        numBlocksInView++;

        // If the first of this patch's blocks have entered the view, activate this patch's terrain so it will be rendered.
        if (numBlocksInView == 1) {
            terrain.gameObject.SetActive(true);
        }
    }

    public void DecrementNumBlocksInView()
    {
        // Decrement the number of this patch's blocks that are in view.
        numBlocksInView--;

        // If the last of this patch's blocks have exited the view, deactivate this patch's terrain so it won't use any rendering resources.
        if (numBlocksInView == 0) {
            terrain.gameObject.SetActive(false);
        }
    }
}
