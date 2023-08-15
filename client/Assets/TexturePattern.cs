using UnityEngine;
using System.Collections;

public class TexturePattern : MonoBehaviour {

	// Use this for initialization
	void Start () {
		Terrain terrain = (Terrain)(this.gameObject.GetComponent ("Terrain"));
		TerrainData terrainData = terrain.terrainData;
		float[,,] alphaMaps = new float[terrainData.alphamapWidth, terrainData.alphamapHeight, terrainData.alphamapLayers];
		for (int z = 0; z < terrainData.alphamapHeight; z++) {
			for (int x = 0; x < terrainData.alphamapWidth; x++) {
				if (((z % 16) == 0) || ((x % 16) == 0)) {
					alphaMaps[x,z,0] = 0f;
				} else {
					alphaMaps[x,z,0] = 1f;
				}
			}
		}
		terrainData.SetAlphamaps(0, 0, alphaMaps);
	}
	
	// Update is called once per frame
	void Update () {
	
	}
}
