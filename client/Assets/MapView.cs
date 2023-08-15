using UnityEngine;
using UnityEngine.Profiling;
using UnityEngine.Analytics;
using System;
using System.Collections;
using System.Collections.Generic;
using System.IO;
using I2.Loc;

// Notes:
// - To avoid log errors "Failed to get pixels of splat texture", set texture type of each terrain texture to "advanced" 
//   and set "read/write enabled" to true. For normal textures, make sure texture type is set to "normal".


// 0: Deep water
// 1: Medium water
// 2: Shallow water
// 3: Beach
// 4: Flat land
// 5: Hills
// 6: Medium mountains
// 7: Tall mountains

public class MapView : MonoBehaviour {

	public const int CHUNK_SIZE = 16;
    public const int CHUNK_SIZE_MINUS_1 = CHUNK_SIZE - 1;
    public const int CHUNK_SIZE_SHIFT = 4;
	public const int HALF_CHUNK_SIZE = CHUNK_SIZE / 2;
	public const float BLOCK_SIZE = 10.0f;
    public const float HALF_BLOCK_SIZE = BLOCK_SIZE / 2f;
	public const int BLOCK_PIXEL_SIZE = 8;
	public const int HALF_BLOCK_PIXEL_SIZE = 4;
	public const int VIEW_DATA_CHUNKS_WIDE = 7;
    public const int VIEW_DATA_CHUNKS_WIDE_LARGE_MULTIPLE = 1024 * VIEW_DATA_CHUNKS_WIDE;
    public const int VIEW_DATA_BLOCKS_WIDE = CHUNK_SIZE * VIEW_DATA_CHUNKS_WIDE + 2;
    public const int VIEW_DATA_BLOCKS_WIDE_LARGE_MULTIPLE = 1024 * VIEW_DATA_BLOCKS_WIDE;
    public const float TERRAIN_PATCH_SIZE = CHUNK_SIZE * BLOCK_SIZE;
    public const float PAUSE_MAP_FOR_UPDATE_MARGIN = 20;
    public const float TERRAIN_PATCH_HEIGHT = 80.0f;
	public const float TERRAIN_SMOOTHING_POWER = 2.0f;
	public const float LAND_LEVEL = 44.0f;
	public const int NOISE_TEXTURE_WIDTH = 512;
	public const int NOISE_TEXTURE_SIZE_MASK = 0x1FF; // 511
	public const float MAX_RANDOM_RANGE = 0.5f;
	public const int HEIGHT_MAP_SIZE = CHUNK_SIZE * BLOCK_PIXEL_SIZE + 1;
	public const int ALPHA_MAP_SIZE = CHUNK_SIZE * BLOCK_PIXEL_SIZE;
	public const int HEIGHT_MAP_LAST = HEIGHT_MAP_SIZE - 1;
	public const int ALPHA_MAP_LAST = ALPHA_MAP_SIZE - 1;
	public const int NUM_TERRAIN_TEXTURES = 10;
	public const float NATION_COLOR_FILL_STRENGTH = 0.4f;

    public const float NATION_BORDER_VALUE = 0.6f;
    public const float CONTENDER_NATION_BORDER_RED_VALUE = 1.0f;
    public const float CONTENDER_NATION_BORDER_GREEN_VALUE = 0.3f;
    public const float CONTENDER_NATION_BORDER_BLUE_VALUE = 0.3f;//0.6f;
    public const float PLAYER_NATION_BORDER_RED_VALUE = 1.0f;
    public const float PLAYER_NATION_BORDER_GREEN_VALUE = 1.0f;
    public const float PLAYER_NATION_BORDER_BLUE_VALUE = 0.3f;

    public const float INITIAL_CAMERA_ZOOM_DISTANCE = 300f;
    public const float MIN_CAMERA_ZOOM_DIST = 63f;
    public const float MAX_CAMERA_ZOOM_DIST = 550f;
    public const int VIEW_AREA_MARGIN = 2;
    public const float SURROUND_COUNT_DISAPPEAR_START_ZOOM_DIST = 110f;
    public const float SURROUND_COUNT_DISAPPEAR_END_ZOOM_DIST = 130f;
    public const float NATION_LABEL_APPEAR_START_ZOOM_DIST = 135f;
    public const float NATION_LABEL_APPEAR_END_ZOOM_DIST = 155f;
    public const float DISPLAY_TIMER_DISAPPEAR_START_ZOOM_DIST = 220f;
    public const float DISPLAY_TIMER_DISAPPEAR_END_ZOOM_DIST = 320f;
    public const float SURROUND_COUNT_FULL_ALPHA = 0.5f;
    public const float NATION_LABEL_FULL_ALPHA = 0.5f;
    public const float DISPLAY_TIMER_FULL_ALPHA = 1.0f;

    private float PAN_CONTINUE_PERIOD = 0.5f;
    private float VIEW_MAX_OUT_OF_BOUNDS = 80f;
    private IntVector2 VIEW_LEFT_BLOCK_MARGIN = new IntVector2(-VIEW_AREA_MARGIN, 0);
    private IntVector2 VIEW_TOP_BLOCK_MARGIN = new IntVector2(0, -VIEW_AREA_MARGIN);
    private IntVector2 VIEW_RIGHT_BLOCK_MARGIN = new IntVector2(VIEW_AREA_MARGIN, 0);
    private IntVector2 VIEW_BOTTOM_BLOCK_MARGIN = new IntVector2(0, VIEW_AREA_MARGIN);

    // Tower actions
    public const float DAMAGE_DURATION = 1.5f;
    public const float DIRECTED_MULTIPLE_AIM_END_TIME = 0.5f;
    public const float DIRECTED_MULTIPLE_FIRE_DURATION = 0.7f;
    public const float SPLASH_AIM_DURATION = 1f;
    public const float SPLASH_FIRE_DURATION = 0.5f;
    public const float AREA_EFFECT_FIRE_DURATION = 1f;
    public const float COUNTER_ATTACK_AIM_DURATION = 0.5f;
    public const float COUNTER_ATTACK_DAMAGE_START_TIME = 1.5f;
    public const float COUNTER_ATTACK_RAID_DURATION = 2.5f;
    public const float ECTO_RAY_FIRE_DURATION = 0.7f;
    public const float DJINN_PORTAL_FIRE_DURATION = 1.8f;
    public const float TOWER_BUSTER_FIRE_DURATION = 3f;
    public const float TOWER_BUSTER_BEGIN_DELAY = 4f;
    public const float ROOTS_OF_DESPAIR_DURATION = 3f;
    public const float AIR_DROP_DAMAGE_START_TIME = 2f;

    // Emblems
    public const int EMBLEM_DIM = 64;
    private const int EMBLEM_DIM_BITMASK = EMBLEM_DIM - 1;
    private const float EMBLEM_INTENSITY = 0.8f;

	public const int TERRAIN_DEEP_WATER 		= 0;
	public const int TERRAIN_MEDIUM_WATER 		= 1;
	public const int TERRAIN_SHALLOW_WATER 		= 2;
	public const int TERRAIN_BEACH 				= 3;
	public const int TERRAIN_FLAT_LAND 			= 4;
	public const int TERRAIN_HILLS 				= 5;
	public const int TERRAIN_MEDIUM_MOUNTAINS 	= 6;
	public const int TERRAIN_TALL_MOUNTAINS 	= 7;
    public const int TERRAIN_POND            	= 8;
    public const int TERRAIN_MOUND              = 9;
	public const int TERRAIN_NUM_TYPES 			= 10;

    public const int UPDATE_BLOCK__MAINTAIN_VALUE = -2;

    public const int TERRAIN_POSITION_QUANTUM = 10;
    public const int TERRAIN_LERP_CACHE_SIZE = 10;

    public const float GARBAGE_COLLECT_INTERVAL = 60f;

    private float TRANS_DUR = 0.2f;
    private float[] transitionPositions;
    private bool transitionInProgress;
    private bool mapPanelView;
    private Vector4[] texture2ModVals;
    private Vector4[] texture3ModVals;
    private Color32[] waterColorVals;
    private float[] waterColorPositions;

    public enum AutoProcessType
    {
        NONE,
        EVACUATE,
        OCCUPY,
        MOVE_TO
    }

    public enum BorderLineType
    {
        NONE,
        NATION_BORDER,
        CONTENDER_NATION_BORDER,
        PLAYER_NATION_BORDER
    };

    public enum PanningType
    {
        NONE,
        MOUSE,
        ONE_TOUCH,
        TWO_TOUCH
    };

    public static MapView instance;

	public GameData gameData;

	private float[] terrainHeights = new float[TERRAIN_NUM_TYPES];

	public Terrain terrainPrefab;
	public TerrainData terrainDataPrefab;
	public Texture2D noiseTexture;
	private int noiseWidth;
    private int noiseHeight;
    private Color[] noisePixels;
    public Camera camera, overlayCamera, infoCamera;
    public Light topDownLight;
    public float avgFrameInterval = 0f;
	private Vector3 cameraVector = Vector3.Normalize (new Vector3(0.7f,-1f,0.7f));
  	private Vector3 cameraAngle = new Vector3(45f,45f,0f);
    private float cameraZoomDistance = 0f;
    private Vector3 pressPosition;
	private float pressTime;
    private PanningType panning = PanningType.NONE;
    private bool viewOutOfBounds = false, panCheckedForHold = false, viewPanned = false, prevViewPanned = false, paused = false;
    private int camera_culling_mask;
    private CameraClearFlags camera_clear_flags;
    private Rect camera_rect;
    private bool initial_map_update_received;
    private bool showGrid = false;
    private const float PAN_HOLD_PERIOD = 0.5f;
	private const float CAMERA_MAX_PAN_SPEED = 400f;
	private const float KEYBOARD_ZOOM_SPEED = 120f;
    private const float EDGE_SCROLL_SPEED = 0.15f;
    private const float KEY_SCROLL_SPEED = 0.25f;
	private const float MAX_VERTICAL_CAMERA_FIELD_OF_VIEW = 32f;
	public Texture2D terrainTexture0, terrainTexture1, terrainTexture2, terrainTexture3, terrainTexture4, terrainTexture5, terrainTexture6, terrainTexture7, terrainTexture8, terrainTexture9;
    public Texture2D grassNormalMap, rockNormalMap;
    public int mapSkin = 0;
    public int mapDimX = Int32.MinValue, mapDimZ = Int32.MinValue;
    public int mapChunkDimX = Int32.MinValue, mapChunkDimZ = Int32.MinValue;

    private int viewBlockX = Int32.MinValue, viewBlockZ = Int32.MinValue, viewChunkX = Int32.MinValue, viewChunkZ = Int32.MinValue;
    private int mapCenterX = 0, mapCenterZ = 0;
    private int mainlandMapCenterX = 0, mainlandMapCenterZ = 0;
	private int viewChunkX0 = Int32.MinValue, viewChunkZ0 = Int32.MinValue;
    public int mapID = -1, sourceMapID = -1;
    public int[,] mapTerrain;
    public float[,] mapBeachDensity;
    public Vector2Int mapDimensions;
    public int viewDataBlockX0 = Int32.MinValue, viewDataBlockZ0 = Int32.MinValue, viewDataBlockX1 = Int32.MinValue, viewDataBlockZ1 = Int32.MinValue;
    public int chunkDataBlockX0 = Int32.MinValue, chunkDataBlockZ0 = Int32.MinValue, chunkDataBlockX1 = Int32.MinValue, chunkDataBlockZ1 = Int32.MinValue;
    public Texture2D terrainTexture2Copy, terrainTexture3Copy;

    // Emblems
    float[,] emblemData = null;
    public int emblemsPerRow = 0;

    // Automatic processes
    private const float AUTO_PROCESS_MAX_TIME = 300f;
    private AutoProcessType autoProcess = AutoProcessType.NONE;
    private int autoProcessBlockX, autoProcessBlockZ, autoProcessMoveX, autoProcessMoveZ, autoProcessEvacX, autoProcessEvacZ, autoProcessLastMoveStepX, autoProcessLastMoveStepZ;
    float autoProcessStartTime = 0f;
    float autoProcessPrevAttemptTime = 0f;
    float autoProcessPrevStepTime = 0f;
    bool autoProcessFirstStep = false;

    // Historical extent of nation and view limits
    public int minX0, minZ0, maxX1, maxZ1;
    public int extraViewRange;
    public float limitX0, limitX1, limitZ0, limitZ1;
    public int limitBlockX0, limitBlockX1, limitBlockZ0, limitBlockZ1;
    public Vector3 prevPanRate = new Vector3(0,0,0);
    public float prevPanTime = 0;

    // Maximum extent of nation, and maximum allowed extent.
    public int mainlandExtentX0, mainlandExtentX1, mainlandExtentZ0, mainlandExtentZ1;
    public int mainlandMaxExtentX0, mainlandMaxExtentX1, mainlandMaxExtentZ0, mainlandMaxExtentZ1;
    public int nationMaxExtent;

    // Periodic garbage collection
    public float prevGarbageCollectTime = 0;
    public float prevLogMemoryUsageTime = 0;

    // Terrain texture modification for view position 
    float terrain_position = -1;
    Color[] texture2Pixels, texture3Pixels;
    Dictionary<Vector3, Color[]> texture2_keys = new Dictionary<Vector3, Color[]>();
    List<Vector3> texture2_lerps_vals = new List<Vector3>();
    List<Color[]> texture2_lerps = new List<Color[]>();
    Dictionary<Vector3, Color[]> texture3_keys = new Dictionary<Vector3, Color[]>();
    List<Vector3> texture3_lerps_vals = new List<Vector3>();
    List<Color[]> texture3_lerps = new List<Color[]>();
    Vector3 texture2CurModVals = new Vector3(0, 0, 0);
    Vector3 texture3CurModVals = new Vector3(0, 0, 0);
    // For testing
    //public float texture2HueMod, texture2SatMod, texture2ValMod, texture3HueMod, texture3SatMod, texture3ValMod;

    // Selection square
    public GameObject smallSelectionSquare, largeSelectionSquare;

    // Water tile objects
    public Renderer waterTile1Rend, waterTile2Rend, waterTile3Rend, waterTile4Rend;

	public BlockData[,] blocks = new BlockData[VIEW_DATA_BLOCKS_WIDE, VIEW_DATA_BLOCKS_WIDE];
	private BlockData[,] blocksTemp = new BlockData[VIEW_DATA_BLOCKS_WIDE, VIEW_DATA_BLOCKS_WIDE];
    public BlockData[,] blocksReplay = null;
    public BlockData[,] blocksReplayOriginal = null;

	private float[,,] alphaMaps;
	private float[] alphaMapCenterHeights = {0.10f, 0.52f, 0.54f, 0.58f, 0.61f, 0.94f, 0.97f}; 

	public PatchData[,] terrainPatches = new PatchData[VIEW_DATA_CHUNKS_WIDE,VIEW_DATA_CHUNKS_WIDE];
	private PatchData[,] terrainPatchesTemp = new PatchData[VIEW_DATA_CHUNKS_WIDE,VIEW_DATA_CHUNKS_WIDE];

	private float[,,] subpatchMixUL = new float[4,4,4];
	private float[,,] subpatchMixUR = new float[4,4,4];
	private float[,,] subpatchMixDL = new float[4,4,4];
	private float[,,] subpatchMixDR = new float[4,4,4];
	private float[,] heights;

    private float startCameraZoomDistance, startTouchDistance;
	private Vector3 worldStartPoint;
	private Vector3 prevTargetPoint = new Vector3(-1,-1,-1);

	private ArrayList modifiedPatchQueue = new ArrayList();

    public IntVector2 view_left_block = new IntVector2(Int32.MinValue, Int32.MinValue);
    public IntVector2 view_top_block = new IntVector2(Int32.MinValue, Int32.MinValue);
    public IntVector2 view_right_block = new IntVector2(Int32.MinValue, Int32.MinValue);
    public IntVector2 view_bottom_block = new IntVector2(Int32.MinValue, Int32.MinValue);

    // Map terrain textures and beach densities

    public Dictionary<int, int[,]> map_terrains = new Dictionary<int, int[,]>();
    public Dictionary<int, float[,]> map_beach_densities = new Dictionary<int, float[,]>();
    public Dictionary<int, Vector2Int> map_dimensions = new Dictionary<int, Vector2Int>();

    // Map GUI

    public Canvas canvas;
    public GameObject buildObjectPrefab;
    public GameObject landscapeObjectPrefab;

    public Woc.ContextMenu contextMenu;

    public Sprite image_stat_bio, image_stat_psi, image_stat_tech;

    // Map labels

    public RectTransform overlay_panel_1_rect_transform, overlay_panel_2_rect_transform;
    public Dictionary<int, NationData> nations_to_label = new Dictionary<int, NationData>();
    public List<SurroundCount> active_surround_counts;
    public List<NationLabel> active_nation_labels;
    public float surround_count_alpha = -1, nation_label_alpha = -1, display_timer_alpha = -1;

    // Replay

    public int replay_map_width = 0, replay_map_height = 0;

    public MapView()
    {
		instance = this;
	}

    // Use this for initialization
    void Start ()
    {
        // Init prevGarbageCollectTime to starting time.
        prevGarbageCollectTime = Time.unscaledTime;

        // Start with the info camera disabled
        infoCamera.enabled = false;

		// Initialize terrain heights array
		terrainHeights[TERRAIN_DEEP_WATER] 			= 0.15f;
		terrainHeights[TERRAIN_MEDIUM_WATER]  		= 0.3f;
		terrainHeights[TERRAIN_SHALLOW_WATER]		= 0.48f;
		terrainHeights[TERRAIN_BEACH] 				= 0.52f;
		terrainHeights[TERRAIN_FLAT_LAND] 			= 0.55f;
		terrainHeights[TERRAIN_HILLS] 				= 0.65f;
		terrainHeights[TERRAIN_MEDIUM_MOUNTAINS] 	= 0.8f;
		terrainHeights[TERRAIN_TALL_MOUNTAINS] 		= 0.9f;
        terrainHeights[TERRAIN_POND]    		    = 0.48f;
        terrainHeights[TERRAIN_MOUND] 		        = 0.60f;

        // Get noise texture dimensions and pixels.
   		noiseWidth = noiseTexture.width;
		noiseHeight = noiseTexture.height;
		noisePixels = noiseTexture.GetPixels (0, 0, noiseWidth, noiseHeight);

		// Initialize terrain subpatch mix matrices.
		InitSubpatchMix(subpatchMixUL, new Vector2(-4f,-4f), new Vector2(4f,-4f), new Vector2(-4f,4f), new Vector2(4f,4f));
		InitSubpatchMix(subpatchMixUR, new Vector2(0f,-4f), new Vector2(8f,-4f), new Vector2(0f,4f), new Vector2(8f,4f));
		InitSubpatchMix(subpatchMixDL, new Vector2(-4f,0f), new Vector2(4f,0f), new Vector2(-4f,8f), new Vector2(4f,8f));
		InitSubpatchMix(subpatchMixDR, new Vector2(0f,0f), new Vector2(8f,0f), new Vector2(0f,8f), new Vector2(8f,8f));
    
        float TRANSDUR1 = 0.075f, TRANSDUR2 = 0.15f;
        float TRANS1 = 0.275f, TRANS2 = 0.55f, TRANS3 = 0.825f, TRANS4 = 0.825f;
        transitionPositions = new float[] { 0, TRANS1 - TRANSDUR2, TRANS1, TRANS2 - TRANSDUR2, TRANS2, TRANS3 - TRANSDUR2, TRANS3, 1 };
        texture2ModVals = new Vector4[] {new Vector4(0,0,0,0), new Vector4(0,0,0,TRANS1 - TRANSDUR1), new Vector4(-0.075f,-0.15f,0.25f,TRANS1),             new Vector4(-0.075f,-0.15f,0.25f,TRANS2 - TRANSDUR1), new Vector4(0.4f,   -0.7f,0.28f,TRANS2),             new Vector4(0.4f,   -0.7f,0.28f,TRANS3 - TRANSDUR1), /*new Vector4(0,-0.5f,-0.45f,TRANS3),             new Vector4(0,-0.5f,-0.45f,TRANS4 - TRANSDUR1),*/ new Vector4(0,0.3f,0.1f,TRANS4),             new Vector4(0,0.3f,0.1f,1)};
        texture3ModVals = new Vector4[] {new Vector4(0,0,0,0), new Vector4(0,0,0,TRANS1 - TRANSDUR2), new Vector4(-0.075f,-0.2f, 0.3f, TRANS1 - TRANSDUR1), new Vector4(-0.075f,-0.2f, 0.3f, TRANS2 - TRANSDUR2), new Vector4(-0.075f,-0.9f,0.5f, TRANS2 - TRANSDUR1), new Vector4(-0.075f,-0.9f,0.5f, TRANS3 - TRANSDUR2), /*new Vector4(0.55f,0,-0.05f,TRANS3 - TRANSDUR1), new Vector4(0.55f,0,-0.05f,TRANS4 - TRANSDUR2),*/ new Vector4(0,0.2f,0.1f,TRANS4 - TRANSDUR1), new Vector4(0,0.2f,0.1f,1)};
        waterColorVals = new Color32[] {new Color32(39,68,76,209), new Color32(39,68,76,209), new Color32(39,68,76,172), new Color32(39,68,76,172), new Color32(122,122,122,209), new Color32(122,122,122,209), new Color32(9,153,174,200), new Color32(9,153,174,200)};
        waterColorPositions = new float[] {0, TRANS1 - TRANSDUR2, TRANS1, TRANS2 - TRANSDUR2, TRANS2, TRANS3 - TRANSDUR2, TRANS3, 1};
    
		// Create alpha maps array
		alphaMaps = new float[ALPHA_MAP_SIZE, ALPHA_MAP_SIZE, NUM_TERRAIN_TEXTURES];

		// Create block data
		for (int z = 0; z < VIEW_DATA_BLOCKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_BLOCKS_WIDE; x++) {
				blocks[x,z] = new BlockData();
			}
		}

        // Make copies of terrain textures 2 and 3, that may be modified.
        terrainTexture2Copy = Instantiate(terrainTexture2) as Texture2D;
        terrainTexture3Copy = Instantiate(terrainTexture3) as Texture2D;

        // Get pixel data for the textures that will need to be modified based on view position.
        texture2Pixels = terrainTexture2.GetPixels();
        texture3Pixels = terrainTexture3.GetPixels();

		// Create terrain patches
		for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
                // Create the new terrain object.
                terrainPatches[x, z] = new PatchData();
                terrainPatches[x,z].terrain = (Terrain) Instantiate(terrainPrefab, new Vector3(0.1f, 0f, 0f), Quaternion.identity); // Set starting position slightly off from the origin, because I found that if a terrain doesn't move at all when its position is later set, it won't draw until the window loses and then regains focus. Hunh.
				
				// Create a new TerrainData from scratch (so that its heights and splat maps can be set individually), and fill in its 
				// attributes and splat maps.
				terrainPatches[x,z].terrain.terrainData = new TerrainData();
				terrainPatches[x,z].terrain.terrainData.heightmapResolution = 129;
				terrainPatches[x,z].terrain.terrainData.size = new Vector3(TERRAIN_PATCH_SIZE,TERRAIN_PATCH_HEIGHT,TERRAIN_PATCH_SIZE);
				terrainPatches[x,z].terrain.terrainData.alphamapResolution = 128;
				terrainPatches[x,z].terrain.terrainData.baseMapResolution = 256;
				terrainPatches[x,z].terrain.terrainData.SetDetailResolution(256, 8);
				SplatPrototype[] splatPrototypes = new SplatPrototype[10];
				for (int i = 0; i < 10; i++) {
					splatPrototypes[i] = new SplatPrototype();
					splatPrototypes[i].tileOffset = new Vector2(0,0);
					splatPrototypes[i].tileSize = new Vector2(20,20);
					switch (i) {
					case 0: splatPrototypes[i].texture = terrainTexture0; splatPrototypes[i].normalMap = rockNormalMap; break;
					case 1: splatPrototypes[i].texture = terrainTexture1; break;
					case 2: splatPrototypes[i].texture = terrainTexture2Copy; break;
					case 3: splatPrototypes[i].texture = terrainTexture3Copy; splatPrototypes[i].normalMap = grassNormalMap; break;
					case 4: splatPrototypes[i].texture = terrainTexture4; splatPrototypes[i].normalMap = rockNormalMap; break;
					case 5: splatPrototypes[i].texture = terrainTexture5; splatPrototypes[i].normalMap = rockNormalMap; break;
					case 6: splatPrototypes[i].texture = terrainTexture6; break;
					case 7: splatPrototypes[i].texture = terrainTexture7; break;
					case 8: splatPrototypes[i].texture = terrainTexture8; break;
					case 9: splatPrototypes[i].texture = terrainTexture9; break;
					}
				}
				terrainPatches[x,z].terrain.terrainData.splatPrototypes = splatPrototypes;
			}
		}

        // Set starting camera zoom distance.
        SetCameraZoomDistance(INITIAL_CAMERA_ZOOM_DISTANCE);

		// Set the camera angle
		camera.transform.rotation.SetFromToRotation(new Vector3(0.0f,0.0f,0.0f), cameraVector);
	}

	// Update is called once per frame
	void Update()
    {
		bool panView = false;
		Vector3 targetPoint = new Vector3(0f,0f,0f);

        prevViewPanned = viewPanned;
        viewPanned = false;

        // Admin camera manipulation and screenshot functions
        if (gameData.userIsAdmin)
        {
            if (Input.GetKeyDown(KeyCode.Home)) {
                StartCoroutine(SnapScreenshot());
            }

            if (Input.GetKey(KeyCode.Keypad7)) {
                camera.transform.rotation = Quaternion.Euler(camera.transform.rotation.eulerAngles + new Vector3(0.5f, 0, 0));
            } else if (Input.GetKey(KeyCode.Keypad9)) {
                camera.transform.rotation = Quaternion.Euler(camera.transform.rotation.eulerAngles + new Vector3(-0.5f, 0, 0));
            }

            if (Input.GetKey(KeyCode.Keypad4)) {
                camera.transform.rotation = Quaternion.Euler(camera.transform.rotation.eulerAngles + new Vector3(0, 0.5f, 0));
            } else if (Input.GetKey(KeyCode.Keypad6)) {
                camera.transform.rotation = Quaternion.Euler(camera.transform.rotation.eulerAngles + new Vector3(0, -0.5f, 0));
            }

            if (Input.GetKey(KeyCode.Keypad8)) {
                camera.transform.localPosition = camera.transform.localPosition + new Vector3(0, 0.5f, 0);
            } else if (Input.GetKey(KeyCode.Keypad5)) {
                camera.transform.localPosition = camera.transform.localPosition + new Vector3(0, -0.5f, 0);
            }
        }

        if (!paused)
        {
            // If input has been received to the map, turn off the alert, if it is active.
            if ((Input.touchCount > 0) || Input.GetMouseButton(0)) {
                Alert.Deactivate();
            }

            if (GameGUI.instance.IsMapFocus())
            {
                // Only respond to mouse input if there is no touch input; because the Input mouse functions also respond to touch input.
                if (Input.touchCount == 0)
                {
		            if (Input.GetMouseButtonDown(0) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject() == false)) 
                    {
                        // Mouse button 0 has just been pressed down, and not over a game object. Begin panning.
			            this.worldStartPoint = prevTargetPoint = this.GetWorldPoint(Input.mousePosition);
			            pressPosition = Input.mousePosition;
			            pressTime = Time.unscaledTime;
			            panning = PanningType.MOUSE;
                        panCheckedForHold = false;
		            }
		            if (Input.GetMouseButton(0) && (panning == PanningType.MOUSE)) 
                    {
                        // Mouse button 0 continues to be pressed down, and panning has already begun. Pan the view given the new target point.
			            targetPoint = this.GetWorldPoint(Input.mousePosition);
			            panView = true;
		            }
		            if (Input.GetMouseButtonUp(0) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject() == false) && (Vector3.Distance(pressPosition, Input.mousePosition) < (Screen.width / 100.0f)) && ((Time.unscaledTime - pressTime) < 0.5f)) 
                    {
                        // Mouse button 0 has been tapped, without dragging. Count this as a click.
                        targetPoint = HandlePointerClick(Input.mousePosition);
		            }
                    if (Input.GetMouseButtonDown(1) && (!Input.GetMouseButton(0)))
                    {
                        // Have right click perform the same action as click-and-hold.
                        HandleClickAndHold(Input.mousePosition);
                    }
                    if (Input.GetMouseButtonDown(2) && (!Input.GetMouseButton(0)))
                    {
                        // Have middle click evacuate.
                        HandleMiddleClick(Input.mousePosition);
                    }
                }
  
                // Handle one-touch panning
		        if (Input.touchCount == 1)
		        {
			        Touch currentTouch = Input.GetTouch(0);

			        if ((panning == PanningType.TWO_TOUCH) || ((currentTouch.phase == TouchPhase.Began) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject(currentTouch.fingerId) == false))) 
                    {
				        this.worldStartPoint = this.GetWorldPoint(currentTouch.position);
				        panning = PanningType.ONE_TOUCH;
                        pressTime = Time.unscaledTime;
                        pressPosition = currentTouch.position;
                        panCheckedForHold = false;
			        }
			        else if (((currentTouch.phase == TouchPhase.Moved) || (currentTouch.phase == TouchPhase.Stationary)) && (panning == PanningType.ONE_TOUCH)) 
                    {
				        targetPoint = this.GetWorldPoint(Input.mousePosition);
				        panView = true;
			        }
                    else if ((currentTouch.phase == TouchPhase.Ended) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject(currentTouch.fingerId) == false) && (Vector3.Distance(pressPosition, currentTouch.position) < (Screen.width / 100.0f)) && ((Time.unscaledTime - pressTime) < 0.5f))
                    {
                        // Touch 0 has been tapped, without dragging. Count this as a click.
                        targetPoint = HandlePointerClick(Input.mousePosition);
                    }
		        }

                // Handle two-touch panning and zooming
		        if (Input.touchCount == 2)
		        {
			        Touch currentTouch0 = Input.GetTouch(0);
                    Touch currentTouch1 = Input.GetTouch(1);
			
			        if ((panning == PanningType.ONE_TOUCH) || ((currentTouch0.phase == TouchPhase.Began) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject(currentTouch0.fingerId) == false)) || ((currentTouch1.phase == TouchPhase.Began) && (UnityEngine.EventSystems.EventSystem.current.IsPointerOverGameObject(currentTouch1.fingerId) == false))) 
                    {
                        // Record starting values. Use the point between the world points of the two touches as the starting point.
				        this.worldStartPoint = Vector3.Lerp(this.GetWorldPoint(currentTouch0.position), this.GetWorldPoint(currentTouch1.position), 0.5f);
                        startCameraZoomDistance = cameraZoomDistance;
                        startTouchDistance = Vector2.Distance(currentTouch0.position, currentTouch1.position);
				        panning = PanningType.TWO_TOUCH;
                        panCheckedForHold = true;
			        }
			        else if (((currentTouch0.phase == TouchPhase.Moved) || (currentTouch0.phase == TouchPhase.Stationary) || (currentTouch1.phase == TouchPhase.Moved) || (currentTouch1.phase == TouchPhase.Stationary)) && (panning == PanningType.TWO_TOUCH)) 
                    {
                        // Use the point between the world points of the two touches as the target point for the pan.
				        targetPoint = Vector3.Lerp(this.GetWorldPoint(currentTouch0.position), this.GetWorldPoint(currentTouch1.position), 0.5f);
				        panView = true;

                        // Determine the new camera zoom distance, based on the ratio of the current distance between the two touches, to the starting distance.
                        float curTouchDistance = Vector2.Distance(currentTouch0.position, currentTouch1.position);
                        Vector3 camera_target_position = camera.transform.position + (cameraVector * cameraZoomDistance);
                        float new_camera_zoom_distance = ((startTouchDistance / curTouchDistance) * startCameraZoomDistance);
                        new_camera_zoom_distance = Math.Min(MAX_CAMERA_ZOOM_DIST, Math.Max(MIN_CAMERA_ZOOM_DIST, new_camera_zoom_distance));
                        Vector3 new_camera_position = camera_target_position - (cameraVector * new_camera_zoom_distance);

                        // Only change zoom if the map panel is not active.
                        if (!mapPanelView)
                        {
                            SetCameraZoomDistance(new_camera_zoom_distance);
                            camera.transform.position = new_camera_position;
                        }
			        }
		        }

		        float zoomDelta = (Input.mouseScrollDelta.y == 0) ? 0f : (Chat.instance.IsMouseOverChatLog() ? 0f : (Input.mouseScrollDelta.y * 10.0f));

                // If the chat input doesn't have focus, up and down arrow keys zoom in and out.
                if (!Chat.instance.chatInputField.isFocused)
                {
		            if (Input.GetKey(KeyCode.UpArrow)) zoomDelta = KEYBOARD_ZOOM_SPEED * Time.unscaledDeltaTime;
		            if (Input.GetKey(KeyCode.DownArrow)) zoomDelta = -KEYBOARD_ZOOM_SPEED * Time.unscaledDeltaTime;
                }

		        if ((zoomDelta != 0f) && (!mapPanelView))
		        {
			        bool shiftPressed = (Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift));

                    // Adjust the zoomDelta to be greater at greater distances.
                    zoomDelta = zoomDelta * (camera.transform.position.y - 50.0f) / 50.0f;

                    // Negate the zoom delta, so as to be able to add to the zoom distance.
                    zoomDelta = -zoomDelta;

                    // Keep the zoom distnce within the allowed bounds.    
                    if (!shiftPressed) {
                        zoomDelta = Math.Min(MAX_CAMERA_ZOOM_DIST - cameraZoomDistance, Math.Max(MIN_CAMERA_ZOOM_DIST - cameraZoomDistance, zoomDelta));
                    }

                    // Change the camera position for this change in zoom.
				    camera.transform.Translate(new Vector3(0f,0f,-zoomDelta));

                    // Record the change to the camera's zoom distance.
                    SetCameraZoomDistance(cameraZoomDistance + zoomDelta);

                    // Update the MapView GUI for this change in view.
                    UpdateMapGUI();

                    // Update the view area.
                    UpdateViewArea();
		        }

                if ((Input.anyKey) && (!GameGUI.instance.InputFieldIsFocused()))
                {
                    float delta = 0, xDegree = 0, zDegree = 0;

                    if (Input.GetKey(KeyCode.A))
                    {
                        delta = -Math.Max(-CAMERA_MAX_PAN_SPEED, cameraZoomDistance * KEY_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree -= delta;
                    }
                    if (Input.GetKey(KeyCode.D))
                    {
                        delta = Math.Max(-CAMERA_MAX_PAN_SPEED, cameraZoomDistance * KEY_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree -= delta;
                    }
                    if (Input.GetKey(KeyCode.W))
                    {
                        delta = Math.Max(-CAMERA_MAX_PAN_SPEED, cameraZoomDistance * KEY_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree += delta;
                    }
                    if (Input.GetKey(KeyCode.S))
                    {
                        delta = -Math.Max(-CAMERA_MAX_PAN_SPEED, cameraZoomDistance * KEY_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree += delta;
                    }

                    if (delta != 0)
                    {
                        // Move the camera to pan.
                        camera.transform.Translate(xDegree, 0, zDegree, Space.World);

                        // Determine whether the view has been panned out of the allowed area.
                        Vector3 centerPoint = GetCameraTargetWorldPoint();
                        if ((centerPoint.x <= limitX0) || (centerPoint.x >= limitX1) || (centerPoint.z >= (-limitZ0)) || (centerPoint.z <= (-limitZ1))) {
                            viewOutOfBounds = true;
                        }

                        // Record that a pan has taken place.
                        viewPanned = true;
                    }
                }

                // Panning by dragging mouse past edge of screen. Only works in fullscreen mode.
                if ((panning == PanningType.NONE) && Screen.fullScreen && (mapPanelView == false))
                {
                    float delta = 0, xDegree = 0, zDegree = 0;

                    if (Input.mousePosition.x == 0) 
                    {
                        delta = Math.Max(-CAMERA_MAX_PAN_SPEED, Math.Min(0, Input.GetAxisRaw("Mouse X")) * cameraZoomDistance * EDGE_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree -= delta;
                    }

                    if (Input.mousePosition.x == (Screen.width - 1)) 
                    {
                        delta = Math.Min(CAMERA_MAX_PAN_SPEED, Math.Max(0, Input.GetAxisRaw("Mouse X")) * cameraZoomDistance * EDGE_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree -= delta;
                    }

                    if (Input.mousePosition.y == 0) 
                    {
                        delta = Math.Max(-CAMERA_MAX_PAN_SPEED, Math.Min(0, Input.GetAxisRaw("Mouse Y")) * cameraZoomDistance * EDGE_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree += delta;
                    }

                    if (Input.mousePosition.y == (Screen.height - 1)) 
                    {
                        delta = Math.Min(CAMERA_MAX_PAN_SPEED, Math.Max(0, Input.GetAxisRaw("Mouse Y")) * cameraZoomDistance * EDGE_SCROLL_SPEED) * Time.unscaledDeltaTime;
                        xDegree += delta;
                        zDegree += delta;
                    }

                    if (delta != 0)
                    {
                        // Move the camera to pan.
                        camera.transform.Translate(xDegree, 0, zDegree, Space.World);

                        // Determine whether the view has been panned out of the allowed area.
                        Vector3 centerPoint = GetCameraTargetWorldPoint();
                        if ((centerPoint.x <= limitX0) || (centerPoint.x >= limitX1) || (centerPoint.z >= (-limitZ0)) || (centerPoint.z <= (-limitZ1))) {
                            viewOutOfBounds = true;
                        }

                        // Record that a pan has taken place.
                        viewPanned = true;
                    }
                }
            }

   		    if ((panning != PanningType.NONE) && (Input.touchCount == 0) && (Input.GetMouseButton(0) == false)) {
			    panning = PanningType.NONE;
		    }

            Vector3 prevCameraPosition = camera.transform.position;

		    if (panView)
		    {
			    // Only move the view if the target position has changed by greater than a certain small amount; this is because raycasting is imprecise, especially far from the world origin, and would otherwise result in jitter.
			    float xDif = Math.Abs(targetPoint.x - prevTargetPoint.x);
			    float zDif = Math.Abs(targetPoint.z - prevTargetPoint.z);
                if ((xDif > (cameraZoomDistance * 0.001f)) || (zDif > (cameraZoomDistance * 0.001f)))
			    {
                    // Determine vector describing how pan target point has moved away from its starting position in the world.
   				    Vector3 worldDelta = targetPoint - this.worldStartPoint;

                    // Clamp the vector's magnitude to the maximum allowed panning speed.
				    if (worldDelta.magnitude > 0.1f) {
					    worldDelta = Vector3.ClampMagnitude(worldDelta, CAMERA_MAX_PAN_SPEED * Time.unscaledDeltaTime);
				    }

                    // Determine whether the view has been panned out of the allowed area.
                    Vector3 centerPoint = GetCameraTargetWorldPoint();
                    if ((centerPoint.x <= limitX0) || (centerPoint.x >= limitX1) || (centerPoint.z >= (-limitZ0)) || (centerPoint.z <= (-limitZ1))) {
                        viewOutOfBounds = true;
                    }

                    //Debug.Log ("Mouse: " + Input.mousePosition + ", world pos under mouse: " + this.GetWorldPoint (Input.mousePosition) + ", worldStartPoint: " + worldStartPoint + ", camera: " + Camera.main.transform.position + ", worldDelta: " + worldDelta);

                    // Move the camera for this pan.
                    camera.transform.Translate (-worldDelta.x, 0, -worldDelta.z, Space.World);

                    // Record that a pan has taken place.
				    viewPanned = true;

                    // If the pan is greater than a small amount...
                    if ((xDif > (cameraZoomDistance * 0.01f)) || (zDif > (cameraZoomDistance * 0.01f)))
			        {
                        // Record rate (camera movement per second) of this latest pan, so the pan will be continued after the player lets go.  
                        prevPanRate = worldDelta * (1 / Time.unscaledDeltaTime);
                        prevPanTime = Time.unscaledTime;
                    }
                    else 
                    {
                        // The pan is for too small an amount to be accurately continued.
                        prevPanTime = 0f;
                    }
                }

                if ((panCheckedForHold == false) && ((Time.unscaledTime - pressTime) >= PAN_HOLD_PERIOD))
                {
                    // If the press has been held in (close to) the same positon for PAN_HOLD_PERIOD...
                    if (Vector3.Distance(pressPosition, Input.mousePosition) < (Screen.width / 50.0f))
                    {
                        targetPoint = this.GetWorldPoint(Input.mousePosition);
                        HandleClickAndHold(Input.mousePosition);
                    }

                    panCheckedForHold = true;
                }
		    }
            else if ((!viewOutOfBounds) && (prevPanTime != 0))
            {
                // Determine the degree to which the previous pan should be continued now. It will trail off over PAN_CONTINUE_PERIOD.
                float degree = Math.Max(0, PAN_CONTINUE_PERIOD - (Time.unscaledTime - prevPanTime));
                if (degree == 0)
                {
                    // The pan continue period since the last pan is over. Stop continuing the pan. 
                    prevPanTime = 0;
                }
                else 
                {
                    // Move the camera to continue the latest pan.
                    Vector3 worldDelta = prevPanRate * degree * Time.unscaledDeltaTime;
                    camera.transform.Translate (-worldDelta.x, 0, -worldDelta.z, Space.World);

                    // Determine whether the view has been panned out of the allowed area.
                    Vector3 centerPoint = GetCameraTargetWorldPoint();
                    if ((centerPoint.x <= limitX0) || (centerPoint.x >= limitX1) || (centerPoint.z >= (-limitZ0)) || (centerPoint.z <= (-limitZ1))) {
                        viewOutOfBounds = true;
                    }

                    // Record that a pan has taken place.
                    viewPanned = true;
                }
            }

            if (viewOutOfBounds && (mapPanelView == false))
            {
                // Determine the degree to which the camera position will be moved back into the allowed area.
                // A small constant amount is added so that it doesn't stop moving until it is firmly back within the allowed area.
                Vector3 centerPoint = GetCameraTargetWorldPoint();
                float degree_out_x = 0f, degree_out_z = 0f;
                if (centerPoint.x < limitX0) degree_out_x = Math.Min(1f, (limitX0 - centerPoint.x) / VIEW_MAX_OUT_OF_BOUNDS) + 0.01f;
                if (centerPoint.x > limitX1) degree_out_x = Math.Max(-1f, (limitX1 - centerPoint.x) / VIEW_MAX_OUT_OF_BOUNDS) - 0.01f;
                if (centerPoint.z < (-limitZ1)) degree_out_z = Math.Min(1f, ((-limitZ1) - centerPoint.z) / VIEW_MAX_OUT_OF_BOUNDS) + 0.01f;
                if (centerPoint.z > (-limitZ0)) degree_out_z = Math.Max(-1f, ((-limitZ0) - centerPoint.z) / VIEW_MAX_OUT_OF_BOUNDS) - 0.01f;

                if ((degree_out_x != 0f) || (degree_out_z != 0f))
                {
                    // Move the camera back in the direction of the allowed area.
                    Vector3 worldDelta = new Vector3(degree_out_x * CAMERA_MAX_PAN_SPEED * Time.unscaledDeltaTime, 0, degree_out_z * CAMERA_MAX_PAN_SPEED * Time.deltaTime);
		            camera.transform.Translate (worldDelta.x, 0, worldDelta.z, Space.World);
			        viewPanned = true;
                }
                else 
                {
                    // The camera is back in the allowed area. Record that it is no longer out of bounds.
                    viewOutOfBounds = false;

                    // Alert the tutorial system that the view is back within bounds.
                    Tutorial.instance.ViewReturnedToBounds();
                }
            }

		    if (viewPanned)
		    {
                // Update the MapView GUI for this change in view.
                UpdateMapGUI();

			    // If the view has panned enough that the target block has changed, update view representation if necessary.
			    Vector3 viewTargetPoint = GetCameraTargetWorldPoint();
			    int newViewBlockX = Math.Min(limitBlockX1, Math.Max(limitBlockX0, (int)(viewTargetPoint.x / BLOCK_SIZE)));
			    int newViewBlockZ = Math.Min(limitBlockZ1, Math.Max(limitBlockZ0, (int)(-(viewTargetPoint.z / BLOCK_SIZE))));
			    if ((newViewBlockX != viewBlockX) || (newViewBlockZ != viewBlockZ)) {
				    PanView(newViewBlockX, newViewBlockZ, false);
			    }

                // Update the view area. This needs to be done after calling PanView(), above, if it is called, so that the appropriate chunks will be marked as pending receiving data from the server.
                UpdateViewArea();
            }
            else if (prevViewPanned)
            {
                // Tell the tutorial system that a view pan has finished.
                Tutorial.instance.ViewPanFinished();
            }

            // Update GUI objects.
            BlockProcess.UpdateAllActivity();
            DisplayAttack.UpdateAllActivity();
            DisplayProcess.UpdateAllActivity();
            DisplayHitPoints.UpdateAllActivity();
            DisplayTimer.UpdateAllActivity();

            // If there is an automatic process running, that hasn't yet been automatically continued due to the previous step completing, continue it if appropriate.
            // This must be done after the displays are updated above, because the end of a displayed process may have triggered am automatic process to continue already.
            if ((autoProcess != AutoProcessType.NONE) && (Time.unscaledTime > (autoProcessPrevAttemptTime + 1f)) && ((DisplayAttack.GetNumActive() + DisplayProcess.GetNumActive()) < GameData.instance.maxSimultaneousProcesses)) {
                ContinueAutoProcess();
            }
        }

        // If a replay is in progress, update it.
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY) {
            UpdateReplay();
        }

        // If any terrain patches are queued to be initialized, initialize just one during this update, to avoid too long of a delay.
        if (modifiedPatchQueue.Count > 0)
		{
            // Determine which patch, queued to be updated, is closest to the view position, to update that patch first.
            PatchUpdate cur_update;
            int closestPatchIndex = 0, closestPatchSqrDist = 1000000, curPatchUpdateSqrDist;
            for (int i = 0; i < modifiedPatchQueue.Count; i++)
            {
                cur_update = (PatchUpdate)(modifiedPatchQueue[i]);
                curPatchUpdateSqrDist = ((cur_update.chunkX - viewChunkX) * (cur_update.chunkX - viewChunkX)) + ((cur_update.chunkZ - viewChunkZ) * (cur_update.chunkZ - viewChunkZ));

                if (curPatchUpdateSqrDist < closestPatchSqrDist)
                {
                    closestPatchIndex = i;
                    closestPatchSqrDist = curPatchUpdateSqrDist;
                }
            }

			// Fetch the chunk coordinates of the next patch to be initialized, and remove it from the queue of modified patches.
			PatchUpdate curUpdate = (PatchUpdate)modifiedPatchQueue[closestPatchIndex];
			modifiedPatchQueue.RemoveAt(closestPatchIndex);
			int patchX = curUpdate.chunkX - viewChunkX0;
			int patchZ = curUpdate.chunkZ - viewChunkZ0;
			int singleBlockX = curUpdate.singleBlockX;
			int singleBlockZ = curUpdate.singleBlockZ;

			//Debug.Log ("Chunk " + patchX + "," + patchZ + " unqueued. View chunk: " + curUpdate.chunkX + "," + curUpdate.chunkZ + ", origin chunk: " + viewChunkX0 + "," + viewChunkZ0);

			// Initialize the patch, if it is still within the view area.
			if ((patchX >= 0) && (patchX < VIEW_DATA_CHUNKS_WIDE) && (patchZ >= 0) && (patchZ < VIEW_DATA_CHUNKS_WIDE)) {
                InitTerrainPatch(patchX, patchZ, singleBlockX, singleBlockZ, curUpdate.setHeights, curUpdate.fullUpdateRequested);
			}

            // If the camera is paused, but the view data has now been completely updated, resume the camera.
            if (paused)
            {
                // Display the progress
                GameGUI.instance.DisplayProgress(modifiedPatchQueue.Count);

                // If there are no more patches to update, resume the camera.
                if (modifiedPatchQueue.Count == 0) 
                {
                    ResumeCamera();
                    
                    // Update the target framerate for being in the game.
                    GameGUI.instance.UpdateTargetFrameRate();
                }
            }
		}

        // Update weighted average of frame interval
        if (!paused) {
            avgFrameInterval = (0.99f * avgFrameInterval) + (0.01f * Mathf.Min(1f, Time.unscaledDeltaTime));
        }

        // If the average frame interval has increased beyond acceptable range (currently lower than 25 fps), and the graphics quality is not yet on the fastest setting...
        if ((avgFrameInterval > 0.04f) && (!GameGUI.instance.userChoseGraphicsQuality) && (GameGUI.instance.graphicsQuality > GameGUI.GraphicsQuality.FASTEST))
        {
            //GameGUI.instance.LogToChat("avgFrameInterval: " + avgFrameInterval + ", setting graphics quality to " + (GameGUI.instance.graphicsQuality - 1));

            // Reduce the graphics quality by one step
            GameGUI.instance.SetGraphicsQuality(GameGUI.instance.graphicsQuality - 1, true);

            // Record that the graphics quality has been automatically reduced.
            PlayerPrefs.SetInt("auto_graphic_quality", (int)GameGUI.instance.graphicsQuality);

            // Reset avgFrameInterval to 0, to start from scratch at this new graphics quality setting.
            avgFrameInterval = 0f;
        }

        // If it's time to garbage collect and the map is not currently being panned...
        if (((Time.unscaledTime - prevGarbageCollectTime) > GARBAGE_COLLECT_INTERVAL) && (panning == PanningType.NONE))
        {
            Debug.Log("About to unload unused assets and garbage collect. Time: " + Time.unscaledTime + ", prevGarbageCollectTime: " + prevGarbageCollectTime);
            // Unload unused assets (this is necessary to destroy cloned materials) and avoid a memory leak.
            //Resources.UnloadUnusedAssets();

            // Garbage collect.
            //System.GC.Collect();
            
            // Record when this most recent garbage collection took place.
            prevGarbageCollectTime = Time.unscaledTime;

            Debug.Log("Finished garbage collection. mono heap size: " + Profiler.GetMonoHeapSizeLong() + ", mono used size: " + Profiler.GetMonoUsedSizeLong() + ", total allocated memory: " + Profiler.GetTotalAllocatedMemoryLong()  + ", total reserved memory: " + Profiler.GetTotalReservedMemoryLong());

            if ((Time.unscaledTime - prevLogMemoryUsageTime) > 3600f)
            {
                GameData.instance.LogMemoryStats();

                // Record when memory usage was most recently logged.
                prevLogMemoryUsageTime = Time.unscaledTime;
            }
        }
	}

    public Vector3 HandlePointerClick(Vector3 _click_position)
    {
        Vector3 targetPoint = this.GetWorldPoint(_click_position);

        // If showing a raid replay, do not respond to click.
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY) {
            return targetPoint;
        }

        IntVector2 targetBlock = this.WorldPointToWorldBlock(targetPoint);
        IntVector2 localBlock = WorldBlockToLocalBlock(targetBlock);
		if ((localBlock.x >= 0) && (localBlock.x < VIEW_DATA_BLOCKS_WIDE) && (localBlock.z >= 0) && (localBlock.z < VIEW_DATA_BLOCKS_WIDE)) 
        {
			// The click occurred within the view area.
			Debug.Log ("Click at x:" + _click_position.x + " y:" + _click_position.y + ", world pos " + targetPoint + " for block " + targetBlock.x + "," + targetBlock.z + ", terrain: " + GetBlockTerrain(targetBlock.x, targetBlock.z));

			// If the click is within the map...
			if ((targetBlock.x >= 0) && (targetBlock.z >= 0) && (targetBlock.x < mapDimX) && (targetBlock.z < mapDimZ))
            {
                BlockData block_data = GetBlockData(targetBlock.x, targetBlock.z);

                if (block_data != null)
                {
                    bool ctrl_pressed = Input.GetKey(KeyCode.LeftControl) || Input.GetKey(KeyCode.RightControl);
                    bool shift_pressed = Input.GetKey(KeyCode.LeftShift) || Input.GetKey(KeyCode.RightShift);
                    bool alt_pressed = Input.GetKey(KeyCode.LeftAlt) || Input.GetKey(KeyCode.RightAlt);

                    //Debug.Log("Admin: " + gameData.userIsAdmin + ", ctrl_pressed: " + ctrl_pressed + ", shift_pressed: " + shift_pressed);

                    if (gameData.userIsAdmin && ctrl_pressed && shift_pressed)
                    {
                        // Handle admin click for block.
                        AdminClick(targetBlock.x, targetBlock.z);
                    }   
                    else if (ctrl_pressed && !shift_pressed)
                    {
                        if (block_data.nationID == GameData.instance.nationID)
                        {
                            if ((block_data.objectID == -1) && (BuildMenu.instance.prevBuildID != -1))
                            {
                                // Build the previously built structure.
                                Woc.ContextMenu.instance.Build(BuildMenu.instance.prevBuildID, targetBlock.x, targetBlock.z);
                            }
                            else if ((block_data.objectID != -1) && (block_data.owner_nationID == GameData.instance.nationID) && ((block_data.completion_time == -1) || (block_data.completion_time <= Time.time)))
                            {
                                // Determine whether there is an upgrade available for the structure in this block.
                                int availableUpgrade = GameData.instance.GetAvailableUpgrade(block_data.objectID);

                                if (availableUpgrade != -1)
                                {
                                    // Send upgrade event to the server.
                                    Woc.ContextMenu.instance.Upgrade(availableUpgrade, targetBlock.x, targetBlock.z);
                                }
                            }
                        }
                    }
                    else if (shift_pressed && (block_data.nationID == GameData.instance.nationID))
                    {
                        // End any auto process that may be in effect.
                        StopAutoProcess();

                        if (ctrl_pressed)
                        {
                            // Evacuate the block (salvaging first if necessary).
                            Woc.ContextMenu.instance.Evacuate(targetBlock.x, targetBlock.z);
                        }
                        else
                        {
                            if ((block_data.objectID != -1) && (block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (block_data.owner_nationID == block_data.nationID))
                            {
                                // The block has a build belonging to this nation in it. Send salvage event to the server.
                                Network.instance.SendCommand("action=salvage|x=" + targetBlock.x + "|y=" + targetBlock.z);
                            }
                            else
                            {
                                // The block has no build belonging to this nation in it. Evacuate the block.
                                Woc.ContextMenu.instance.Evacuate(targetBlock.x, targetBlock.z);
                            }
                        }
                    }
                    else if (alt_pressed)
                    {
                        if (block_data.nationID != GameData.instance.nationID)
                        {
                            if (IsEligibleForAutoProcess(MapView.AutoProcessType.MOVE_TO, targetBlock.x, targetBlock.z))
                            {
                                // End any auto process that may be in effect.
                                StopAutoProcess();

                                // Start the automatic process of moving to a square.
                                StartAutoProcess(MapView.AutoProcessType.MOVE_TO, targetBlock.x, targetBlock.z);
                            }
                        }
                    }
                    else if (block_data.nationID == gameData.nationID)
                    {
                        // End any auto process that may be in effect.
                        StopAutoProcess();

                        // Activate the context menu for the block that has been clicked on.
                        ActivateContextMenu(targetBlock.x, targetBlock.z);
                    }
                    else
                    {
                        if ((IsBlockAdjacentToNation(targetBlock.x, targetBlock.z) == false)/* || (block_data.nationID == -1)*/)
                        {
                            ActivateContextMenu(targetBlock.x, targetBlock.z);
                        }
                        else
                        {
                            // End any auto process that may be in effect.
                            StopAutoProcess();

                            // If the block is not currently locked...
                            if (block_data.locked_until <= Time.unscaledTime)
                            {
                                // If the number of processes (including attacks) is less than the maximum allowed...
                                if ((DisplayAttack.GetNumActive() + DisplayProcess.GetNumActive()) < GameData.instance.maxSimultaneousProcesses)
                                {
                                    if ((block_data.nationID == -1) || (block_data.nationID == gameData.nationID) || !GameData.instance.FealtyPreventsAction())
                                    {
                                        // Send a mapclick event to the server.
                                        Network.instance.SendCommand("action=mapclick|x=" + targetBlock.x + "|y=" + targetBlock.z + (shift_pressed ? "|splash=1" : ""));

                                        // Record that this user event has occurred
                                        if (block_data.nationID == -1) GameData.instance.UserEventOccurred(GameData.UserEventType.OCCUPY);
                                    }
                                }
                            }
                            else
                            {
                                Debug.Log("MapClick: On locked block " + targetBlock.x + "," + targetBlock.z + ". Locked until: " + block_data.locked_until + ", cur unscaled time: " + Time.unscaledTime);
                            }
                        }
                    }
                }
                else
                {
                    Debug.Log("MapClick: BlockData is NULL at " + targetBlock.x + "," + targetBlock.z + ".");
                }
            }
            else
            {
                Debug.Log("MapClick: Click outside map at " + targetBlock.x + "," + targetBlock.z + ". mapDimX: " + mapDimX + ", mapDimZ: " + mapDimZ + ".");
            }
        }

        return targetPoint;
    }

    public void HandleClickAndHold(Vector3 _click_position)
    {
        // If showing a raid replay, do not respond to click.
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY) {
            return;
        }

        Vector3 targetPoint = this.GetWorldPoint(Input.mousePosition);
        IntVector2 targetBlock = this.WorldPointToWorldBlock(targetPoint);

        // The click occurred within the view area.
		//Debug.Log ("Click-and-hold (or right click) at x:" + Input.mousePosition.x + " y:" + Input.mousePosition.y + ", world pos " + targetPoint + " for block " + targetBlock.x + "," + targetBlock.z + ", terrain: " + GetBlockTerrain(targetBlock.x, targetBlock.z));

        //// End any auto process that may be in effect.
        //StopAutoProcess();
                        
        BlockData block_data = GetBlockData(targetBlock.x, targetBlock.z);

        if (block_data != null)
        {
            // Activate the context menu for the block that has been clicked on and held.
            if (ActivateContextMenu(targetBlock.x, targetBlock.z, true))
            {
                // Abort the panning.
                //panView = false;
                panning = PanningType.NONE;
            }
        }
    }

    public void HandleMiddleClick(Vector3 _click_position)
    {
        // If showing a raid replay, do not respond to click.
        if (GameData.instance.mapMode == GameData.MapMode.REPLAY) {
            return;
        }

        IntVector2 targetBlock = this.WorldPointToWorldBlock(this.GetWorldPoint(Input.mousePosition));
        BlockData blockData = GetBlockData(targetBlock.x, targetBlock.z);
        if ((blockData != null) && (blockData.nationID == GameData.instance.nationID)) {
            Network.instance.SendCommand("action=evacuate|x=" + targetBlock.x + "|y=" + targetBlock.z);
        }
    }

    public void InfoEventReceived()
    {
        // Reset for receiving a new map.
        ResetMap();
    }

    public void ResetMap()
    {
        // Reset map view position information
        viewBlockX = Int32.MinValue;
        viewBlockZ = Int32.MinValue;
        viewChunkX = Int32.MinValue;
        viewChunkZ = Int32.MinValue;
        viewChunkX0 = Int32.MinValue;
        viewChunkZ0 = Int32.MinValue;
        viewDataBlockX0 = Int32.MinValue;
        viewDataBlockZ0 = Int32.MinValue;
        viewDataBlockX1 = Int32.MinValue;
        viewDataBlockZ1 = Int32.MinValue;
        chunkDataBlockX0 = Int32.MinValue;
        chunkDataBlockZ0 = Int32.MinValue;
        chunkDataBlockX1 = Int32.MinValue;
        chunkDataBlockZ1 = Int32.MinValue;
        view_left_block = new IntVector2(Int32.MinValue, Int32.MinValue);
        view_top_block = new IntVector2(Int32.MinValue, Int32.MinValue);
        view_right_block = new IntVector2(Int32.MinValue, Int32.MinValue);
        view_bottom_block = new IntVector2(Int32.MinValue, Int32.MinValue);

        // Clear block data (do this before clearing any remaining nation labels and surround counts, below).
		for (int z = 0; z < VIEW_DATA_BLOCKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_BLOCKS_WIDE; x++) {
                blocks[x, z].Clear();
			}
		}
        
        // Clear any previously existing nation labels from the map.
        GameObject nation_label_object;
        while (active_nation_labels.Count > 0)
        {
            nation_label_object = active_nation_labels[0].gameObject;
            active_nation_labels.RemoveAt(0);
            NationLabel nationLabel = nation_label_object.GetComponent<NationLabel>();
            nationLabel.nationID = -1;
            nation_label_object.SetActive(false);
            MemManager.instance.ReleaseNationLabelObject(nation_label_object);
        }

        // Clear any previously existing surround counts from the map.
        GameObject surround_count_object;
        while (active_surround_counts.Count > 0)
        {
            surround_count_object = active_surround_counts[0].gameObject;
            active_surround_counts.RemoveAt(0);
            surround_count_object.SetActive(false);
            MemManager.instance.ReleaseSurroundCountObject(surround_count_object);
        }

        // Initialize each terrain patch: mark as pending both data and an update, and clear AudioSources.
        for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
                terrainPatches[x, z].Init(true);
            }
        }

        // Record that the initial map update event has not yet been received.
        initial_map_update_received = false;

        // Record that we're not in map panel view
        mapPanelView = false;

        // Determine the view position limits.
        DetermineViewLimits();
    }

    public void AccountInfoEventReceived()
    {
        // If there is a label for the user's nation, update it to display the user's nation's current name (which may have changed).
        if ((gameData.nationID != -1) && (gameData.nationTable.ContainsKey(gameData.nationID)))
        {
            NationData nation_data = gameData.nationTable[gameData.nationID];
            if ((nation_data != null) && (nation_data.label != null)) {
                nation_data.label.GetComponent<NationLabel>().SetText(gameData.nationName);
            }
        }
    }

    public void DetermineViewLimits()
    {
        if ((mapID != GameData.MAINLAND_MAP_ID) || gameData.userIsAdmin)
        {
            limitBlockX0 = 0;
            limitBlockZ0 = 0;
            limitBlockX1 = (mapDimX - 1);
            limitBlockZ1 = (mapDimZ - 1);
        }
        else
        {
            limitBlockX0 = Math.Max(0, minX0 - extraViewRange);
            limitBlockZ0 = Math.Max(0, minZ0 - extraViewRange);
            limitBlockX1 = Math.Min(mapDimX - 1, maxX1 + extraViewRange);
            limitBlockZ1 = Math.Min(mapDimZ - 1, maxZ1 + extraViewRange);
        }

        limitX0 = limitBlockX0 * BLOCK_SIZE;
        limitZ0 = limitBlockZ0 * BLOCK_SIZE;
        limitX1 = limitBlockX1 * BLOCK_SIZE;
        limitZ1 = limitBlockZ1 * BLOCK_SIZE;

        //Debug.Log("DetermineViewLimits() minX0: " + minX0 + ", minZ0: " + minZ0 + ", maxX1: " + maxX1 + ", maxZ1: " + maxZ1 + ", limitX0: " + limitX0 + ", limitZ0: " + limitZ0 + ", limitX1: " + limitX1 + ", limitZ1: " + limitZ1 + ", userIsAdmin: " + gameData.userIsAdmin + ", mapDimX: " + mapDimX + ", limitBlockX1: " + limitBlockX1);
    }

    public void PauseCamera(bool _freeze_image)
    {
        // If already paused, do nothing.
        if (paused) {
            return;
        }

        camera_culling_mask = camera.cullingMask;
        camera_clear_flags = camera.clearFlags;
        camera_rect = camera.rect;

        if (_freeze_image) 
        {
            // Set the camera to full screen, otherwise it will display a message when paused.
            camera.rect = new Rect(0f, 0f, 1f, 1f);
            overlayCamera.rect = camera.rect;

            camera.clearFlags = CameraClearFlags.Nothing;

            // Hide semi-transparent parts of the GUI that would otherwise blow out while the camera isn't clearing its image.
            GameGUI.instance.mapLocationTextObject.SetActive(false);
            GameGUI.instance.compassRoseRectTransform.gameObject.SetActive(false);
            GameGUI.instance.statusBarsLeftRectTransform.gameObject.SetActive(false);
            GameGUI.instance.statusBarsRightRectTransform.gameObject.SetActive(false);
            GameGUI.instance.messageTextRectTransform.gameObject.SetActive(false);
            Chat.instance.gameObject.SetActive(false);
        }

        camera.cullingMask = 0;

        // Deactivate the overlay camera
        overlayCamera.gameObject.SetActive(false);

        // Pause game sound
        Sound.instance.ExitPlay();

        // Record that the camera has been paused.
        paused = true;
    }

    public void ResumeCamera()
    {
        // If not paused, do nothing.
        if (!paused) {
            return;
        }

        camera.clearFlags = camera_clear_flags;
        camera.cullingMask = camera_culling_mask;

        // Reset the camera view to its previous size and position.
        camera.rect = camera_rect;
        overlayCamera.rect = camera.rect;

        // Re-show semi-transparent parts of the GUI that would otherwise blow out while the camera isn't clearing its image.
        GameGUI.instance.compassRoseRectTransform.gameObject.SetActive(GameData.instance.mapMode == GameData.MapMode.MAINLAND);
        Chat.instance.gameObject.SetActive(true);
        GameGUI.instance.messageTextRectTransform.gameObject.SetActive(true);

        // Only re-show the stat bars if we're not displaying a replay map.
        if (GameData.instance.mapMode != GameData.MapMode.REPLAY)
        {
            GameGUI.instance.statusBarsLeftRectTransform.gameObject.SetActive(true);
            GameGUI.instance.statusBarsRightRectTransform.gameObject.SetActive(true);
        }

        // Show map location text if appropriate
        if (gameData.GetUserFlag(GameData.UserFlags.SHOW_MAP_LOCATION)) 
        {
            GameGUI.instance.mapLocationTextObject.SetActive(true);
            MapView.instance.UpdateMapLocationText();
        }

        // Reactivate the overlay camera
        overlayCamera.gameObject.SetActive(true);

        // Resume game sound
        Sound.instance.EnterPlay();

        // Record that the camera is no longer paused.
        paused = false;
    }

    public bool IsCameraPaused()
    {
        return paused;
    }

	public Vector3 GetWorldPoint(Vector2 _screenPoint) {
		// Use layer mask to detect collision between ray and the water's collider, so that all collisions are at the same y level.
		RaycastHit hit;
		Physics.Raycast (Camera.main.ScreenPointToRay(_screenPoint), out hit, Mathf.Infinity, 1 << 4);
		return hit.point;
	}

    public Vector3 GetWorldPointFromViewportPosition(Vector2 _viewportPoint) {
		// Use layer mask to detect collision between ray and the water's collider, so that all collisions are at the same y level.
		RaycastHit hit;
		Physics.Raycast (Camera.main.ViewportPointToRay(_viewportPoint), out hit, Mathf.Infinity, 1 << 4);
		return hit.point;
	}

	public Vector3 GetCameraTargetWorldPoint() {
		// Use layer mask to detect collision between ray and the water's collider, so that all collisions are at the same y level.
		RaycastHit hit;
		Physics.Raycast (Camera.main.transform.position, Camera.main.transform.forward, out hit, Mathf.Infinity, 1 << 4);
		return hit.point;
	}

    public void SetView(int _mapID, int _sourceMapID, int _skin, int _mapDimX, int _mapDimZ, int _viewX, int _viewZ, bool _pan_view, bool _replay)
	{
        // Determine whether to pan the view and reposition the camera. Do so if we've switched to a different map, or moved a great distance on the same map.
        //bool doPanView = (_pan_view || (mapID != _mapID) || (Math.Abs(_viewX - viewBlockX) > VIEW_DATA_BLOCKS_WIDE) || (Math.Abs(_viewZ - viewBlockZ) > VIEW_DATA_BLOCKS_WIDE));
        bool doPanView = _pan_view;

        // Record whether we are switching to a different map.
        bool switching_map = (_mapID != mapID);

        Debug.Log("SetView() _mapID: " + _mapID + ", _sourceMapID: " + _sourceMapID + ", _skin: " + _skin + ", _viewX: " + _viewX + ", _viewZ: " + _viewZ + ", viewBlockX: " + viewBlockX + ", viewBlockZ: " + viewBlockZ + ", doPanView: " + doPanView + ", replay: " + _replay + ", prev mapID: " + mapID + ", switching_map: " + switching_map);

        // If switching to a different map...
        if (switching_map) 
        {
            // Have all blocks exit the view area. This will remove all objects, wipes, process bars, etc.
            UpdateViewArea(true);
        }

        // Discontinue any camera transition that may be in progress.
        transitionInProgress = false;

        // Record map ID and dimensions
        mapID = _mapID;
        sourceMapID = _sourceMapID;
        mapSkin = _skin;
		mapDimX = _mapDimX;
		mapDimZ = _mapDimZ;
        mapCenterX = mapDimX / 2;
        mapCenterZ = mapDimZ / 2;
        mapChunkDimX = (_mapDimX + CHUNK_SIZE - 1) / CHUNK_SIZE;
        mapChunkDimZ = (_mapDimZ + CHUNK_SIZE - 1) / CHUNK_SIZE;

        // Cache pointer to map's terrain data and beach density data.
        mapTerrain = map_terrains[sourceMapID];
        mapBeachDensity = map_beach_densities[sourceMapID];
        mapDimensions = map_dimensions[sourceMapID];

        if (_replay)
        {
            if ((mapDimX != replay_map_width) || (mapDimZ != replay_map_height))
            {
                replay_map_width = mapDimX;
                replay_map_height = mapDimZ;

                // Make sure that the replay block data is the appropriate size.
                blocksReplay = new BlockData[mapDimX, mapDimZ];
                blocksReplayOriginal = new BlockData[mapDimX, mapDimZ];

                for (int z = 0; z < mapDimZ; z++) {
			        for (int x = 0; x < mapDimX; x++) 
                    {
				        blocksReplay[x,z] = new BlockData();
                        blocksReplayOriginal[x,z] = new BlockData();
			        }
		        }
            }

            // Clear the replay original map block data.
            for (int z = 0; z < mapDimZ; z++) {
                for (int x = 0; x < mapDimX; x++) {
                    blocksReplayOriginal[x, z].Clear();
                }
            }

            // Set view to center of the map.
            _viewX = mapDimX / 2;
            _viewZ = mapDimZ / 2;
        }

        // Reset terrain_position so map skin will be redetermined.
        terrain_position = -1;

        // Re-determine the view limits
        DetermineViewLimits();

        if (switching_map)  
        {
            // Reset for receiving a new map.
            ResetMap();

            // Stop any auto-process that may be going on.
            StopAutoProcess();

            // Update the GameData for having switched map.
            GameData.instance.OnSwitchedMap(mapID, _replay);

            // Hide the stat bars if the map is paused or if we're displaying a replay map. Otherwise show them.
            bool show_stat_bars = (GameData.instance.mapMode != GameData.MapMode.REPLAY) && !paused;
            GameGUI.instance.statusBarsLeftRectTransform.gameObject.SetActive(show_stat_bars);
            GameGUI.instance.statusBarsRightRectTransform.gameObject.SetActive(show_stat_bars);
        }

		if (doPanView)
		{
			// Pan the view to the given target location block.
			PanView(_viewX, _viewZ, true);

			// Position the camera to view the block being viewed.
            camera.transform.position = GetCameraPositionForTargetBlock(_viewX, _viewZ);

            // Queue all patches to be updated.
            QueueAllPatchesModified(true);

            // Update the MapView GUI for this change in view.
            UpdateMapGUI();

            // Update the view area.
            UpdateViewArea();
        }
	}

    public void InitReplay()
    {
        // Copy block data from blockReplayOrginal to blockReplay.
        for (int z = 0; z < mapDimZ; z++) {
            for (int x = 0; x < mapDimX; x++) 
            {
                blocksReplay[x, z].Copy(blocksReplayOriginal[x, z]);

                if ((x >= viewDataBlockX0) && (x < (viewDataBlockX0 + VIEW_DATA_BLOCKS_WIDE)) && (z >= viewDataBlockZ0) && (z < (viewDataBlockZ0 + VIEW_DATA_BLOCKS_WIDE))) {
                    blocks[x - viewDataBlockX0, z - viewDataBlockZ0].Copy(blocksReplayOriginal[x, z]);
                }
            }
        }

        // Remove all blocks from the view area, and re-add them. This resets all objects, wipes, block process displays, etc.
        UpdateViewArea(true);
        UpdateViewArea();
    }

	public void PanView(int _viewX, int _viewZ, bool _server_initiated)
	{
		// If the view center block has not changed, do nothing. Return.
		if ((_viewX == viewBlockX) && (_viewZ == viewBlockZ)) {
			return;
		}

		// Record the new view block
		viewBlockX = _viewX;
		viewBlockZ = _viewZ;

		// Determine the chunk in which the view centered block falls.
		viewChunkX = WorldBlockToGlobalChunkX(_viewX);
        viewChunkZ = WorldBlockToGlobalChunkZ(_viewZ);
	
		// Determine the new view area origin.
        // The view center is kept within chunks 2,3 through 3,4 relative to the view origin. This is off-center because a greater distance is visible on the far sides of both axes.
		int newViewChunkX0, newViewChunkZ0;
		if (viewChunkX0 == Int32.MinValue) { 
			// The view is not yet positioned on this map.
			// Reset view area origin so that view is centered in chunk 2,3.
			newViewChunkX0 = viewChunkX - 2;
			newViewChunkZ0 = viewChunkZ - 3;
		} else {
			// If necessary, adjust view area origin so that view is centered within chunks 2,3 through 3,4.
			newViewChunkX0 = ((viewChunkX - viewChunkX0) < 2) ? (viewChunkX - 2) : (((viewChunkX - viewChunkX0) > 3) ? (viewChunkX - 3) : viewChunkX0);
			newViewChunkZ0 = ((viewChunkZ - viewChunkZ0) < 3) ? (viewChunkZ - 3) : (((viewChunkZ - viewChunkZ0) > 4) ? (viewChunkZ - 4) : viewChunkZ0);
		}

		//Debug.Log ("For viewChunkX: " + viewChunkX + ", viewChunkZ: " + viewChunkZ + ", newViewChunkX0: " + newViewChunkX0 + ", newViewChunkZ0: " + newViewChunkZ0);

		//// Determine the new view origin chunk position.
		//// Choose the chunk in which to position the view such that the view is as close as possible to centered.
		//int newViewChunkX0 = viewChunkX - (((viewBlockX % CHUNK_SIZE) < HALF_CHUNK_SIZE) ? 2 : 1);
		//int newViewChunkZ0 = viewChunkZ - (((viewBlockZ % CHUNK_SIZE) < HALF_CHUNK_SIZE) ? 2 : 1);

		// If the view area origin is changing its chunk position...
		if ((newViewChunkX0 != viewChunkX0) || (newViewChunkZ0 != viewChunkZ0))
        {
			int shiftX, shiftZ, originalX, originalZ, finalOriginalX, finalOriginalZ;
            bool inside_map;

			// If the previous view position was valid, shift terrain to new view position.
			if (viewChunkX0 != Int32.MinValue)
			{
				// Shift terrain patches matrix.
				shiftX = newViewChunkX0 - viewChunkX0;
				shiftZ = newViewChunkZ0 - viewChunkZ0;
				for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) 
                {
					for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) 
                    {
                        // Determine the original position for the current block, pre-shift.

                        // Add shift
                        originalX = x + shiftX;
                        originalZ = z + shiftZ;

                        // Bound values to >= 0
                        finalOriginalX = originalX + VIEW_DATA_CHUNKS_WIDE_LARGE_MULTIPLE;
                        finalOriginalZ = originalZ + VIEW_DATA_CHUNKS_WIDE_LARGE_MULTIPLE;

                        // Bound values to < VIEW_DATA_CHUNKS_WIDE
                        finalOriginalX = finalOriginalX % VIEW_DATA_CHUNKS_WIDE;
                        finalOriginalZ = finalOriginalZ % VIEW_DATA_CHUNKS_WIDE;

                        //Debug.Log("x: " + x + ", z: " + z + ", originalX: " + originalX + ", originalZ: " + originalZ);

                        terrainPatchesTemp[x,z] = terrainPatches[finalOriginalX, finalOriginalZ];

                        // If the patch has been shifted to the other side of the view, queue it to be updated and record that it is pending data and an update.
                        if ((originalX != finalOriginalX) || (originalZ != finalOriginalZ))
                        {
                            QueueModifiedPatch(newViewChunkX0 + x, newViewChunkZ0 + z, -1, -1, true);

                            inside_map = ((x + newViewChunkX0) >= 0) && ((x + newViewChunkX0) < mapChunkDimX) && ((z + newViewChunkZ0) >= 0) && ((z + newViewChunkZ0) < mapChunkDimZ);

                            // Initialize terrain patch: mark as pending both data and an update, and clear AudioSources.
                            terrainPatchesTemp[x, z].Init(inside_map);
							//Debug.Log ("For patch shifting from " + finalOriginalX + "," + finalOriginalZ + " to " + x + "," + z + ", patch queued for update. shiftX: " + shiftX + ", shiftZ: " + shiftZ);
						}
					}
				}
				
				// Swap the old with the new terrain patches matrix.
				PatchData[,] tempT = terrainPatchesTemp;
				terrainPatchesTemp = terrainPatches;
				terrainPatches = tempT;

                // Shift block data
                int source_x, source_z, final_source_x, final_source_z;
                shiftX *= CHUNK_SIZE;
				shiftZ *= CHUNK_SIZE;
				for (int z = 0; z < VIEW_DATA_BLOCKS_WIDE; z++) 
                {
					for (int x = 0; x < VIEW_DATA_BLOCKS_WIDE; x++) 
                    {
                        source_x = x + shiftX;
                        source_z = z + shiftZ;
                        final_source_x = (source_x + VIEW_DATA_BLOCKS_WIDE_LARGE_MULTIPLE) % VIEW_DATA_BLOCKS_WIDE;
                        final_source_z = (source_z + VIEW_DATA_BLOCKS_WIDE_LARGE_MULTIPLE) % VIEW_DATA_BLOCKS_WIDE;

                        // Get this block's data from its determined source block.
                        blocksTemp[x,z] = blocks[final_source_x, final_source_z];

                        // If this block has flipped around to the other side, clear its data.
                        if ((source_x != final_source_x) || (source_z != final_source_z)) {
                            blocksTemp[x,z].Clear();
                        }
					}
				}

				// Swap the old with the new block data matrix.
				BlockData[,] tempB = blocksTemp;
				blocksTemp = blocks;
				blocks = tempB;
			}
            else
            {
                // Initialize all chunk patches
                for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) 
                {
					for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) 
                    {
                        // Determine whether this chunk is within the map
                        inside_map = ((x + newViewChunkX0) >= 0) && ((x + newViewChunkX0) < mapChunkDimX) && ((z + newViewChunkZ0) >= 0) && ((z + newViewChunkZ0) < mapChunkDimZ);
                        //Debug.Log("Patch " + x + "," + z + " (view chunk " + (x + newViewChunkX0) + "," + (z + newViewChunkZ0) + ") inside_map: " + inside_map + ". Map chunk dims: " + mapChunkDimX + "," + mapChunkDimZ);

                        // Initialize terrain patch: mark as pending both data and an update, and clear AudioSources.
                        terrainPatches[x, z].Init(inside_map);
                    }
                }
            }

			//Debug.Log ("PanView() Changing viewChunkX0 from " + viewChunkX0 + " to " + newViewChunkX0 + " and viewChunkZ0 from " + viewChunkZ0 + " to " + newViewChunkZ0 + ". viewChunkZ: " + viewChunkZ + ", viewBlockZ: " + viewBlockZ);

			// Record the new view area position.
			viewChunkX0 = newViewChunkX0;
			viewChunkZ0 = newViewChunkZ0;
            chunkDataBlockX0 = viewChunkX0 * CHUNK_SIZE;
            chunkDataBlockZ0 = viewChunkZ0 * CHUNK_SIZE;
            chunkDataBlockX1 = chunkDataBlockX0 + (VIEW_DATA_CHUNKS_WIDE * CHUNK_SIZE) - 1;
            chunkDataBlockZ1 = chunkDataBlockZ0 + (VIEW_DATA_CHUNKS_WIDE * CHUNK_SIZE) - 1;
            // Note that the block data extends 1 block further in each direction than what the chunks represent.
			viewDataBlockX0 = chunkDataBlockX0 - 1; 
			viewDataBlockZ0 = chunkDataBlockZ0 - 1;
			viewDataBlockX1 = chunkDataBlockX1 + 1;
			viewDataBlockZ1 = chunkDataBlockZ1 + 1;

            if ((!_server_initiated) && (GameData.instance.mapMode != GameData.MapMode.REPLAY))
            {
			    // Let server know about change in view position, so it can send back landscape data. Note that where client uses z axis, server uses y axis.
			    Network.instance.SendCommand("action=pan_view|x=" + _viewX + "|y=" + _viewZ, true);
                //Debug.Log("Sent event: action=pan_view|x=" + _viewX + "|y=" + _viewZ);
                //Debug.Log(System.Environment.StackTrace);

                // Record that this user event has occurred
                GameData.instance.UserEventOccurred(GameData.UserEventType.PAN_MAP);
            }

			// Reposition the terrain patches.
			PositionTerrainPatches();

            // Make any GUI changes necessary given that the local map area has changed.
            BlockProcess.UpdateAllForMapAreaChange();
            DisplayAttack.UpdateAllForMapAreaChange();
            DisplayProcess.UpdateAllForMapAreaChange();
            DisplayHitPoints.UpdateAllForMapAreaChange();
            DisplayTimer.UpdateAllForMapAreaChange();
        }

        // Update the map location text if necessary.
        if (gameData.GetUserFlag(GameData.UserFlags.SHOW_MAP_LOCATION)) {
            UpdateMapLocationText();
        }

        // Update the terrain textures for the part of the world being viewed, if necessary.
        UpdateTerrainForViewPosition();

        // Update sound for local ocean density
        UpdateSoundForZoomDistance(); // Make sure this is set first.
        UpdateSoundForLocalOceanDensity();
    }

    public void UpdateMapLocationText()
    {
        if (mapCenterX != 0) {
            GameGUI.instance.mapLocationText.text = GetMapLocationText(viewBlockX, viewBlockZ, false);
        }
    }

    public String GetMapLocationText(int _x, int _z, bool _mainland)
    {
        int centerX = _mainland ? mainlandMapCenterX : mapCenterX;
        int centerZ = _mainland ? mainlandMapCenterZ : mapCenterZ;

        return Math.Abs(_z - centerZ) + ((_z <= centerZ) ? " N, " : " S, ") + Math.Abs(_x - centerX) + ((_x <= centerX) ? " W" : " E");
    }

    public void SetShowGrid(bool _showGrid)
    {
        bool prevShowGrid = showGrid;
        showGrid = _showGrid;

        if (prevShowGrid != showGrid) {
            QueueAllPatchesModified(false); // Queue all map patches to be updated, with new grid setting.
        }
    }

    // This is only used to move the camera locally, not requiring any change in chunk position.
   	public void LocalPanView(int _viewX, int _viewZ)
    {
        // Discontinue any camera transition that may be in progress.
        transitionInProgress = false;

  		// Record the new view block
		viewBlockX = _viewX;
		viewBlockZ = _viewZ;

   		// Position the camera to view the block being viewed.
        camera.transform.position = GetCameraPositionForTargetBlock(_viewX, _viewZ);

        // Update the MapView GUI for this change in view.
        UpdateMapGUI();

        // Update the view area.
        UpdateViewArea();
    }

    public Vector3 GetCameraPositionForTargetBlock(int _viewX, int _viewZ)
    {
		Vector3 targetVector = new Vector3(((float)_viewX + 0.5f) * BLOCK_SIZE, LAND_LEVEL, -((float)_viewZ + 0.5f) * BLOCK_SIZE);
		Vector3 cameraPosVector = (-cameraVector) * cameraZoomDistance + targetVector;
        return cameraPosVector;
    }

    public bool BlockOutsideViewData(int _x, int _z)
    {
        return ((_x < viewDataBlockX0) || (_x > viewDataBlockX1) || (_z < viewDataBlockZ0) || (_z > viewDataBlockZ1));
    }

    public BlockData GetBlockData(int _x, int _z)
    {
        // If the given block is not represented in the view, return null.
        if ((_x < viewDataBlockX0) || (_x > viewDataBlockX1) || (_z < viewDataBlockZ0) || (_z > viewDataBlockZ1)) {
            return null;
        }

        return blocks[_x - viewDataBlockX0, _z - viewDataBlockZ0];
    }

    public void LockBlock(int _x, int _z, float _lock_until)
    {
        BlockData block_data = GetBlockData(_x, _z);
        if (block_data != null) {
            block_data.locked_until = _lock_until;
        }

        // Let the context menu know that the given block has been modified.
        contextMenu.BlockModified(_x, _z);
    }

	public void UpdateBlock(int _x, int _z, int _nationID, bool _fast_crumble)
	{
        //Debug.Log("UpdateBlock() called for " + _x + "," + _z + ", _nationID " + _nationID);
        BlockData blockData = GetBlockData(_x, _z);

        if (blockData == null) {
            return;
        }

        // Record whether the block has an object owned by the block's owner, before the block is captured.
        bool object_prev_owned_by_owner = (blockData.objectID != -1) && (blockData.nationID == blockData.owner_nationID);

        // TESTING
        //Debug.Log("UpdateBlock(" + _x + "," + _z + ") cur nationID: " + blockData.nationID + ", setting to nationID " + _nationID);

        // If the block is changing owner, update the block to reflect this change.
        if (_nationID != blockData.nationID) 
        {
		    int localX = _x - viewDataBlockX0;
		    int localZ = _z - viewDataBlockZ0;
		    int localChunkX = WorldBlockToLocalChunkX(_x);
		    int localChunkZ = WorldBlockToLocalChunkZ(_z);
            int worldChunkX = viewChunkX0 + localChunkX;
            int worldChunkZ = viewChunkZ0 + localChunkZ;
		    int localChunkOriginX = localChunkX * CHUNK_SIZE + 1;
		    int localChunkOriginZ = localChunkZ * CHUNK_SIZE + 1;
		    int blockXRelToChunk = localX - localChunkOriginX;
		    int blockZRelToChunk = localZ - localChunkOriginZ;
            int prevNationID = blocks[localX, localZ].nationID;

		    // Update the changed data for this block
		    if (_nationID != UPDATE_BLOCK__MAINTAIN_VALUE) blocks[localX, localZ].nationID = _nationID;

		    // Update the patch that the updated block lies within.
		    QueueModifiedPatch(worldChunkX, worldChunkZ, blockXRelToChunk, blockZRelToChunk, false);

		    // Update patch to the left if it is adjacent to the updated block.
		    if ((localChunkX > 0) && (blockXRelToChunk == 0)) {
			    QueueModifiedPatch(worldChunkX - 1, worldChunkZ, CHUNK_SIZE, blockZRelToChunk, false);
		    }

		    // Update patch to the right if it is adjacent to the updated block.
		    if ((localChunkX < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockXRelToChunk == (CHUNK_SIZE - 1))) {
			    QueueModifiedPatch(worldChunkX + 1, worldChunkZ, 0, blockZRelToChunk, false);
		    }

		    // Update patch above if it is adjacent to the updated block.
		    if ((localChunkZ > 0) && (blockZRelToChunk == 0)) {
			    QueueModifiedPatch(worldChunkX, worldChunkZ - 1, blockXRelToChunk, CHUNK_SIZE, false);
		    }

		    // Update patch below if it is adjacent to the updated block.
		    if ((localChunkZ < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockZRelToChunk == (CHUNK_SIZE - 1))) {
			    QueueModifiedPatch(worldChunkX, worldChunkZ + 1, blockXRelToChunk, 0, false);
		    }

		    // Update patch to upper left if it is adjacent to the updated block.
		    if ((localChunkX > 0) && (blockXRelToChunk == 0) && (localChunkZ > 0) && (blockZRelToChunk == 0)) {
			    QueueModifiedPatch(worldChunkX - 1, worldChunkZ - 1, CHUNK_SIZE, CHUNK_SIZE, false);
		    }

		    // Update patch to upper right if it is adjacent to the updated block.
		    if ((localChunkX < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockXRelToChunk == (CHUNK_SIZE - 1)) && (localChunkZ > 0) && (blockZRelToChunk == 0)) {
			    QueueModifiedPatch(worldChunkX + 1, worldChunkZ - 1, 0, CHUNK_SIZE, false);
		    }

		    // Update patch to bottom left if it is adjacent to the updated block.
		    if ((localChunkZ < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockZRelToChunk == (CHUNK_SIZE - 1)) && (localChunkX > 0) && (blockXRelToChunk == 0)) {
			    QueueModifiedPatch(worldChunkX - 1, worldChunkZ + 1, CHUNK_SIZE, 0, false);
		    }

		    // Update patch to bottom right if it is adjacent to the updated block.
		    if ((localChunkZ < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockZRelToChunk == (CHUNK_SIZE - 1)) && (localChunkX < (VIEW_DATA_CHUNKS_WIDE - 1)) && (blockXRelToChunk == (CHUNK_SIZE - 1))) {
			    QueueModifiedPatch(worldChunkX + 1, worldChunkZ + 1, 0, 0, false);
		    }

            // Let the context menu know that the given block has been modified.
            contextMenu.BlockModified(_x, _z);

            // If this block had a nation label, remove it.
            if (blockData.label_nationID != -1) {
                RemoveLabelFromBlock(blockData);
            }

            // Place a nation label in this block if appropriate.
            if (_nationID != -1) 
            {
                EvaluateBlockForNationLabel(blockData, _x, _z);
                CreateNationLabels();
            }

            // If this is a block with an Orb in it, and it is being captured by or from the player's nation, update the game data.
            if ((blockData.objectID >= ObjectData.ORB_BASE_ID) && ((_nationID == GameData.instance.nationID) || (prevNationID == GameData.instance.nationID))) {
                GameData.instance.OrbChangedHands(_x, _z, (_nationID == GameData.instance.nationID));
            }

            // Update the block's and its neighbors' surround counts.
            UpdateNeighboringSurroundCounts(_x, _z, true);
        }

        // Record whether the block has an object owned by the block's owner, after the block is captured.
        bool object_cur_owned_by_owner = (blockData.objectID != -1) && (blockData.nationID == blockData.owner_nationID);

        // If the block has a build object and has just been captured from the object's owner, or re-captured by the object's owner...
        if ((blockData.objectID != -1) && (blockData.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (object_prev_owned_by_owner != object_cur_owned_by_owner))
        {
            // Get the block's BuildData
            BuildData build_data = BuildData.GetBuildData(blockData.objectID);

            // Record the time at which the block was captured.
            blockData.capture_time = Time.time;

            // If the block is no longer owned by the object's owner, and is not a shard, record the object's crumble time. If re-captured by owner, clear crumble time.
            blockData.crumble_time = (object_cur_owned_by_owner || (build_data.type == BuildData.Type.SHARD)) ? -1 : Time.time + (_fast_crumble ? GameData.instance.timeUntilFastCrumble : GameData.instance.timeUntilCrumble);

            //Debug.Log("UpdateBlock(" + _x + "," + _z + ") object_prev_owned_by_owner: " + object_prev_owned_by_owner + ", object_cur_owned_by_owner: " + object_cur_owned_by_owner + ", cur nationID: " + blockData.nationID);

            if (blockData.build_object != null) 
            {
                // Display that the block has been captured.
                blockData.build_object.Captured();

                // If the build object has a storage meter, update it.
                if (blockData.build_object.storageMeter != null) {
                    blockData.build_object.storageMeter.GetComponent<StorageMeter>().UpdateMeter(blockData);
                }
            }
        }
    }

    public void ProcessEnded()
    {
        // If there is an automatic process running, and we're not using all our allowed simultaneous processes, continue the automatic process if appropriate.
        if ((autoProcess != AutoProcessType.NONE) && ((DisplayAttack.GetNumActive() + DisplayProcess.GetNumActive()) < GameData.instance.maxSimultaneousProcesses)) {
            ContinueAutoProcess();
        }
    }

    // Queue for update all chunks that include any blocks occupied by the nation with the given _nationID.
    public void NationDataReceived(int _nationID)
    {
        //Debug.Log("NationDataReceived(" + _nationID + ")");

        Boolean chunk_queued;

        // Iterate through all local chunks...
        for (int chunkZ = 0; chunkZ < VIEW_DATA_CHUNKS_WIDE; chunkZ++) 
        {
			for (int chunkX = 0; chunkX < VIEW_DATA_CHUNKS_WIDE; chunkX++) 
            {
                chunk_queued = false;
                int blockX0 = viewDataBlockX0 + 1 + (chunkX * CHUNK_SIZE);
                int blockX1 = blockX0 + CHUNK_SIZE;
                int blockZ0 = viewDataBlockZ0 + 1 + (chunkZ * CHUNK_SIZE);
                int blockZ1 = blockZ0 + CHUNK_SIZE;

                // Iterate through all blocks in this local chunk...
                for (int blockZ = blockZ0; blockZ < blockZ1; blockZ++) 
                {
			        for (int blockX = blockX0; blockX < blockX1; blockX++) 
                    {
                        // Get this block's data.
                        BlockData blockData = GetBlockData(blockX, blockZ);

                        // If the block's data wan't found, log error and skip it.
                        if (blockData == null)
                        {
                            Debug.Log("ERROR: NationDataReceived(): no data found for block " + blockX + "," + blockZ + ". viewDataBlockX0: " + viewDataBlockX0 + ", viewDataBlockZ0 : " + viewDataBlockZ0);
                            continue;
                        }

                        // If this block is occupied by the nation with the given _nationID, queue this chunk's patch to be updated, and go on to the next chunk.
                        if (blockData.nationID == _nationID)
                        {
                            QueueModifiedPatch(viewChunkX0 + chunkX, viewChunkZ0 + chunkZ, -1, -1, false);
                            chunk_queued = true;
                            break;
                        }
                    }

                    if (chunk_queued) {
                        break;
                    }
                }
            }
        }
    }

    void PositionTerrainPatches()
	{
		// Position all terrain patches
		for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
				terrainPatches[x,z].terrain.transform.position = new Vector3((viewChunkX0 + x) * TERRAIN_PATCH_SIZE, 0.0f, (-((viewChunkZ0 + z) * TERRAIN_PATCH_SIZE) - TERRAIN_PATCH_SIZE)); // Use negative z so that world coordinates are the negative of map coordinates, rather than needing to subtract the map coordiante from the map height to determine the world coordinate.
				// TESTING 
				//if ((z!=2) || (x!=2)) terrainPatches[x,z].terrain.transform.position = new Vector3(1000.0f,1000.0f,1000.0f);
				//Debug.Log ("Terrain " + x + "," + z + ": " + terrainPatches[x,z].terrain.transform.position.x + "," + terrainPatches[x,z].terrain.transform.position.y + "," + terrainPatches[x,z].terrain.transform.position.z);
			}
		}
	}

	public void AreaDataUpdateComplete(int _blockUpdateX0, int _blockUpdateZ0, int _blockUpdateX1, int _blockUpdateZ1)
	{
		int x, z;

		//Debug.Log ("AreaDataUpdateComplete() called for update to area from " + _blockUpdateX0 + "," + _blockUpdateZ0 + " to " + _blockUpdateX1 + "," + _blockUpdateZ1);

        // For each terrain patch that is overlapping the area of blocks that have been updated by at least two rows:
        //   - Mark that terrain patch as no longer pending reception of its data.
        //   - Queue that terrain patch as being modified, so that its data will be displayed.
        // This used to be done for each terrain patch that overlaps the updated area of blocks at all, but this caused a problem. The server sends an "extra" block of perimeter around the entire view area. That extra block was causing the chunk that contains it to update, which was then preventing updates of that chunk when the actual full data for that chunk was later received.
        // So then it was changed to update each terrain patch whose center was contained by the updated area of blocks. That fixed the previous problem, but it meant that patches at the edge of the map that had very few rows of map data in them would never be updated, would remain pending, and so would never appear in the view.
		for (z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
//				if ((_blockUpdateX0 < ((viewChunkX0 + x + 1) * CHUNK_SIZE)) &&
//				    (_blockUpdateX1 >= ((viewChunkX0 + x) * CHUNK_SIZE)) && 
//				    (_blockUpdateZ0 < ((viewChunkZ0 + z + 1) * CHUNK_SIZE)) &&
//				    (_blockUpdateZ1 >= ((viewChunkZ0 + z) * CHUNK_SIZE))) 
//				if ((_blockUpdateX0 < ((viewChunkX0 + x) * CHUNK_SIZE + HALF_CHUNK_SIZE)) &&
//				    (_blockUpdateX1 >= ((viewChunkX0 + x) * CHUNK_SIZE + HALF_CHUNK_SIZE)) && 
//				    (_blockUpdateZ0 < ((viewChunkZ0 + z) * CHUNK_SIZE + HALF_CHUNK_SIZE)) &&
//				    (_blockUpdateZ1 >= ((viewChunkZ0 + z) * CHUNK_SIZE + HALF_CHUNK_SIZE))) 
                if ((_blockUpdateX0 < ((viewChunkX0 + x + 1) * CHUNK_SIZE - 1)) &&
				    (_blockUpdateX1 >= ((viewChunkX0 + x) * CHUNK_SIZE + 1)) && 
				    (_blockUpdateZ0 < ((viewChunkZ0 + z + 1) * CHUNK_SIZE - 1)) &&
				    (_blockUpdateZ1 >= ((viewChunkZ0 + z) * CHUNK_SIZE + 1))) 
				{
                    // Data is no longer pending being received for this chunk.
                    terrainPatches[x, z].pendingData = false;

                    // Update the view area for having received the data for this chunk.
                    UpdateViewAreaForChunkDataReceived(x, z);

                    // Queue the patch to be updated. No need to update the terrain height values, since that update was queued previously. 
                    // The received data just updates the nation ID values that show in the texture colors.
					QueueModifiedPatch(viewChunkX0 + x, viewChunkZ0 + z, -1, -1, false);
                    //Debug.Log ("For updated blocks " + _blockUpdateX0 + "," + _blockUpdateZ0 + " to " + _blockUpdateX1 + "," + _blockUpdateZ1 + ", patch " + x + "," + z + " for chunk " + (viewChunkX0 + x) + "," + (viewChunkZ0 + z) + " queued to be updated.");
				}
			}
		}

        // If we've received map data for an area that comes close enough to the view block, pause the camera until the view has been completely updated.
        // Only freeze the current view if the initial map update has previously been received. For the initial map update, continue clearing the view.
        if ((_blockUpdateX0 <= (viewBlockX + PAUSE_MAP_FOR_UPDATE_MARGIN)) && (_blockUpdateX1 >= (viewBlockX - PAUSE_MAP_FOR_UPDATE_MARGIN)) && (_blockUpdateZ0 <= (viewBlockZ + PAUSE_MAP_FOR_UPDATE_MARGIN)) && (_blockUpdateZ1 >= (viewBlockZ - PAUSE_MAP_FOR_UPDATE_MARGIN))) 
        {
            PauseCamera(initial_map_update_received);
            GameGUI.instance.DisplayProgress(modifiedPatchQueue.Count);
        }

        // Record that the initial map update event has been received.
        initial_map_update_received = true;

        // Now that UpdateViewAreaForChunkDataReceived() has been called for all chunk data received, call UpdateViewAreaComplete();
        UpdateViewAreaComplete();
	}

    public void QueueAllPatchesModified(bool _setHeights)
    {
        // Queue all patches to be updated.
        for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
				for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
                QueueModifiedPatch(viewChunkX0 + x, viewChunkZ0 + z, -1, -1, _setHeights);
            }
        }
    }

	void QueueModifiedPatch(int _world_chunkX, int _world_chunkZ, int _singleBlockX, int _singleBlockZ, bool _setHeights)
	{
        //Debug.Log ("QueueModifiedPatch() called for _world_chunkX: " + _world_chunkX + ", _world_chunkZ: " + _world_chunkZ + ", _singleBlockX: " + _singleBlockX + ", _singleBlockZ: " + _singleBlockZ);

        PatchUpdate cur_update;
        
        // Check whether this chunk has already been queued...
        for (int i = 0; i < modifiedPatchQueue.Count; i++)
        {
            cur_update = (PatchUpdate)(modifiedPatchQueue[i]);

            // If an update for the given chunk is already queued...
            if ((_world_chunkX == cur_update.chunkX) && (_world_chunkZ == cur_update.chunkZ))
            {
                // If the queued update is for a single block, and this update is either for a different block or for the whole chunk, modify the queued update to be for the whole chunk.
                if ((cur_update.singleBlockX != -1) && ((_singleBlockX != cur_update.singleBlockX) || (_singleBlockZ != cur_update.singleBlockZ)))
                {
                    cur_update.singleBlockX = -1;
                    cur_update.singleBlockZ = -1;
                }

                // If the queued update is not to set heights, but the given update is to set heights, modify the queued update to set heights.
                if ((cur_update.setHeights == false) && _setHeights) {
                    cur_update.setHeights = true;
                }

                // If the requested update is for the full chunk rather than a single block, record in this update record that a full update has been requested.
                if (_singleBlockX == -1) {
                    cur_update.fullUpdateRequested = true;
                }

                // A PatchUpdate for the given chunk already exists in the queue. Do not add a new one.
                return;
            }
        }

  		// Queue the patch with the given coordinates to be re-initialized, for having had its data modified.
		// Coordinates are recorded relative to the map origin, in case the view moves.
		PatchUpdate patchUpdate = new PatchUpdate();
		patchUpdate.chunkX = _world_chunkX;
		patchUpdate.chunkZ = _world_chunkZ;
		patchUpdate.singleBlockX = _singleBlockX;
		patchUpdate.singleBlockZ = _singleBlockZ;
        patchUpdate.setHeights = _setHeights;
        patchUpdate.fullUpdateRequested = (_singleBlockX == -1);

        // Insert the new PatchUpdate into the modifiedPatchQueue. Order doesn't matter, updates will be made starting from closest to view position.
		modifiedPatchQueue.Add(patchUpdate);
	}

	void InitTerrainPatch( int _x, int _z, int _singleBlockX, int _singleBlockZ, bool _set_heights, bool _fullUpdateRequested)
    {
		NationData nationData;
		int x, z, x0, z0;

        //if (_singleBlockX == 4) Debug.Log("InitTerrainPatch() called for patch " + _x + ", " + _z + ", _singleBlockX: " + _singleBlockX + ", _singleBlockZ: " + _singleBlockZ + ", _set_heights: " + _set_heights);

		// Determine whether we're performing an update of a single block.
		bool singleBlockUpdate = (_singleBlockX != -1);

		PatchData terrainPatch = terrainPatches[_x,_z];
		int localX0 = _x * CHUNK_SIZE + 1;
		int localZ0 = _z * CHUNK_SIZE + 1;

        int terrainX0 = viewDataBlockX0 + localX0;
        int terrainZ0 = viewDataBlockZ0 + localZ0;

        //Debug.Log("InitTerrainPatch() called for world chunk X: " + (_x + viewChunkX0) + ", world chunk z: " + (_z + viewChunkZ0));
		//Debug.Log ("InitTerrainPatch() for patch " + _x + "," + _z + " at position " + terrainPatch.terrain.GetPosition().x + "," + terrainPatch.terrain.GetPosition().z + ": _localX0: " + localX0 + ", _localZ0: " + localZ0 + ", terrainX0: " + terrainX0 + ", terrainZ0: " + terrainZ0 + ", _singleBlockX: " + _singleBlockX + ", _singleBlockZ: " + _singleBlockZ);

		int noiseOriginX = (_x + viewChunkX0) * CHUNK_SIZE * BLOCK_PIXEL_SIZE;
		int noiseOriginZ = (_z + viewChunkZ0) * CHUNK_SIZE * BLOCK_PIXEL_SIZE;

		TerrainData terrainData = terrainPatch.terrain.terrainData;
		int xResolution = terrainData.heightmapResolution;
		int zResolution = terrainData.heightmapResolution;

        if (_set_heights) {
    		heights = terrainData.GetHeights (0, 0, xResolution, zResolution);
        }

		// Clear the alpha maps array
		Array.Clear (alphaMaps, 0, ALPHA_MAP_SIZE * ALPHA_MAP_SIZE * NUM_TERRAIN_TEXTURES);

		float ul_height, uc_height, ur_height, cl_height, cc_height, cr_height, dl_height, dc_height, dr_height;
		BlockData blockData;
		bool borderDirU, borderDirD, borderDirL, borderDirR, borderDirUL, borderDirUR, borderDirDL, borderDirDR; 
		float borderUL, borderDL, borderUR, borderDR, borderU, borderD, borderL, borderR, borderC;
        BorderLineType borderLineU, borderLineL, borderLineUL;
		bool log; // TESTING

		int blockX0 = singleBlockUpdate ? Math.Max(0, (_singleBlockX - 1)) : 0;
		int blockZ0 = singleBlockUpdate ? Math.Max(0, (_singleBlockZ - 1)) : 0;
		int blockX1 = singleBlockUpdate ? Math.Min(CHUNK_SIZE, (_singleBlockX + 1)) : CHUNK_SIZE;
		int blockZ1 = singleBlockUpdate ? Math.Min(CHUNK_SIZE, (_singleBlockZ + 1)) : CHUNK_SIZE;

        int playerNationID = GameData.instance.nationID;

        if ((gameData.mapMode == GameData.MapMode.REPLAY) && _fullUpdateRequested)
        {
            // Copy data for each block from the blockReplay array, that contains the entire replay map.
            // Only do this when initializing the terrain for the whole chunk, not just a single block (or multiple single blocks that have become a full chunk update). 
            // Values for single blocks are changed by individual updates, and changing them first here would prevent those updates and cause problems (such as border lines being left behind, towers remaining grayed out after being recaptured).
            for (int blockZ = blockZ0; blockZ <= blockZ1; blockZ++) 
		    {
                for (int blockX = blockX0; blockX <= blockX1; blockX++) 
			    {
                    BlockData destBlock = blocks[localX0 + blockX, localZ0 + blockZ];
                    int terrainX = terrainX0 + blockX;
                    int terrainZ = terrainZ0 + blockZ;

                    if ((terrainX >= 0) && (terrainX < replay_map_width) && (terrainZ >= 0) && (terrainZ < replay_map_height))
                    {
                        // This block is within the replay map; copy it over.
                        //Debug.Log("terrain: " + terrainX0 + "," + terrainZ0 + ", block: " + blockX + "," + blockZ + " 0: " + blockX0 + "," + blockZ0 + " 1: " + blockX1 + "," + blockZ1);
                        BlockData sourceBlock = blocksReplay[terrainX, terrainZ];
                        destBlock.Copy(sourceBlock);
                        //if ((terrainX == 24) && (terrainZ == 35)) Debug.Log("InitTerrainPatch() copied over data for block " + terrainX + "," + terrainZ);
                    }
                    else
                    {
                        // This block is outside of the replay map; clear it.
                        destBlock.Clear();
                    }
                }
            }
        }

		// Note that we're processing one block beyond the right and bottom of this patch's chunk, so as to fill in the rightmost
		// and bottommost height and alpha values (and have them match those of the adjoining terrain patch). To accommodate this,
		// the far corner values for those right most and bottommost blocks, which aren't needed, are just set to 0.
		for (int blockZ = blockZ0; blockZ <= blockZ1; blockZ++) 
		{
			z0 = Math.Max(0, blockZ * BLOCK_PIXEL_SIZE);

            uc_height = terrainHeights[GetBlockTerrain(terrainX0 + blockX0 - 1, terrainZ0 + blockZ - 1)];
			cc_height = terrainHeights[GetBlockTerrain(terrainX0 + blockX0 - 1, terrainZ0 + blockZ)];
			dc_height = (blockZ == CHUNK_SIZE) ? 0.0f : terrainHeights[GetBlockTerrain(terrainX0 + blockX0 - 1, terrainZ0 + blockZ + 1)];
			ur_height = terrainHeights[GetBlockTerrain(terrainX0 + blockX0, terrainZ0 + blockZ - 1)];
			cr_height = terrainHeights[GetBlockTerrain(terrainX0 + blockX0, terrainZ0 + blockZ)];
			dr_height = (blockZ == CHUNK_SIZE) ? 0.0f : terrainHeights[GetBlockTerrain(terrainX0 + blockX0, terrainZ0 + blockZ + 1)];

			for (int blockX = blockX0; blockX <= blockX1; blockX++) 
			{
				ul_height = uc_height;
				cl_height = cc_height;
				dl_height = dc_height;
				uc_height = ur_height;
				cc_height = cr_height;
				dc_height = dr_height;

                ur_height = (blockX == CHUNK_SIZE) ? 0.0f : terrainHeights[GetBlockTerrain(terrainX0 + blockX + 1, terrainZ0 + blockZ - 1)];
				cr_height = (blockX == CHUNK_SIZE) ? 0.0f : terrainHeights[GetBlockTerrain(terrainX0 + blockX + 1, terrainZ0 + blockZ)];
				dr_height = ((blockX == CHUNK_SIZE) || (blockZ == CHUNK_SIZE)) ? 0.0f : terrainHeights[GetBlockTerrain(terrainX0 + blockX + 1, terrainZ0 + blockZ + 1)];

				x0 = Math.Max(0, blockX * BLOCK_PIXEL_SIZE);

				blockData = blocks[localX0 + blockX, localZ0 + blockZ];
                //if (blockData.nationID != -1) Debug.Log ("blockData.nationID: " + blockData.nationID);// TESTING

                // TEMPORARY -- For finding sporadic bug where client receives messed up map data.
                if ((blockData.nationID != -1) && (gameData.nationTable.ContainsKey(blockData.nationID) == false))
                {
                    Debug.Log("Block " + (viewDataBlockX0 + localX0 + blockX) + "," +  (viewDataBlockZ0 + localZ0 + blockZ) + " has nationID " + blockData.nationID + " that is not in the nation table."); 

                    // Send analytics custom event
                    Analytics.CustomEvent("blockUnknownNationID", new Dictionary<string, object>
                    {
                        { "x", (viewDataBlockX0 + localX0 + blockX) },
                        { "z", (viewDataBlockZ0 + localZ0 + blockZ) },
                        { "nationID", blockData.nationID }
                    });

                    // TEMPORARY: Set the block's nationID to -1, so as to avoid causing an exception.
                    blockData.nationID = -1;
                }

				nationData = (blockData.nationID == -1) ? null : gameData.nationTable[blockData.nationID];

				borderDirU = (blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID != blockData.nationID);
				borderDirD = (blockZ == CHUNK_SIZE) ? false : (blocks[localX0 + blockX, localZ0 + blockZ + 1].nationID != blockData.nationID);
				borderDirL = (blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID != blockData.nationID);
				borderDirR = (blockX == CHUNK_SIZE) ? false : (blocks[localX0 + blockX + 1, localZ0 + blockZ].nationID != blockData.nationID);
				borderDirUL = (blocks[localX0 + blockX - 1, localZ0 + blockZ - 1].nationID != blockData.nationID);
				borderDirUR = (blockX == CHUNK_SIZE) ? false : (blocks[localX0 + blockX + 1, localZ0 + blockZ - 1].nationID != blockData.nationID);
				borderDirDL = (blockZ == CHUNK_SIZE) ? false : (blocks[localX0 + blockX - 1, localZ0 + blockZ + 1].nationID != blockData.nationID);
				borderDirDR = ((blockX == CHUNK_SIZE) || (blockZ == CHUNK_SIZE)) ? false : (blocks[localX0 + blockX + 1, localZ0 + blockZ + 1].nationID != blockData.nationID);
				borderUL = (borderDirU || borderDirL || borderDirUL) ? 1.0f : NATION_COLOR_FILL_STRENGTH;
				borderUR = (borderDirU || borderDirR || borderDirUR) ? 1.0f : NATION_COLOR_FILL_STRENGTH;
				borderDL = (borderDirD || borderDirL || borderDirDL) ? 1.0f : NATION_COLOR_FILL_STRENGTH;
				borderDR = (borderDirD || borderDirR || borderDirDR) ? 1.0f : NATION_COLOR_FILL_STRENGTH;
				borderL = (borderUL + borderDL) / 2f;
				borderR = (borderUR + borderDR) / 2f;
				borderU = (borderUL + borderUR) / 2f;
				borderD = (borderDL + borderDR) / 2f;
				borderC = Math.Min(0.99f, Math.Max((borderU + borderD) / 2f, (borderL + borderR) / 2f));

                borderLineU = borderDirU ? (((blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : ((IsNationContender(blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID) || IsNationContender(blockData.nationID)) ? BorderLineType.CONTENDER_NATION_BORDER : BorderLineType.NATION_BORDER)) : BorderLineType.NONE;
                borderLineL = borderDirL ? (((blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : ((IsNationContender(blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID) || IsNationContender(blockData.nationID)) ? BorderLineType.CONTENDER_NATION_BORDER : BorderLineType.NATION_BORDER)) : BorderLineType.NONE;
                borderLineUL = borderDirUL ? (((blocks[localX0 + blockX - 1, localZ0 + blockZ - 1].nationID == playerNationID) || (blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID == playerNationID) || (blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : ((IsNationContender(blocks[localX0 + blockX - 1, localZ0 + blockZ - 1].nationID) || IsNationContender(blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID) || IsNationContender(blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID) || IsNationContender(blockData.nationID)) ? BorderLineType.CONTENDER_NATION_BORDER : BorderLineType.NATION_BORDER)) : BorderLineType.NONE;

                //if (_singleBlockX == 4) Debug.Log("  block " + blockX + "," + blockZ + ": borderLineU: " + borderLineU);

                //borderLineU = borderDirU ? (((blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : BorderLineType.NATION_BORDER) : BorderLineType.NONE;
                //borderLineL = borderDirL ? (((blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : BorderLineType.NATION_BORDER) : BorderLineType.NONE;
                //borderLineUL = borderDirUL ? (((blocks[localX0 + blockX - 1, localZ0 + blockZ - 1].nationID == playerNationID) || (blocks[localX0 + blockX, localZ0 + blockZ - 1].nationID == playerNationID) || (blocks[localX0 + blockX - 1, localZ0 + blockZ].nationID == playerNationID) || (blockData.nationID == playerNationID)) ? BorderLineType.PLAYER_NATION_BORDER : BorderLineType.NATION_BORDER) : BorderLineType.NONE;

				log = false;
				//log = singleBlockUpdate;
				//if (((localX0 + blockX + viewDataBlockX0) == 2) && ((localZ0 + blockZ + viewDataBlockZ0) == 0)) {
				//	log = true;
				//}

				InitSubpatch(localX0 + blockX, localZ0 + blockZ, x0, x0 + 4, z0, z0 + 4, ul_height, uc_height, cl_height, cc_height, subpatchMixUL, noisePixels, noiseWidth, noiseHeight, nationData, blockData.nationID, borderUL, borderU, borderL, borderC, borderLineU, borderLineL, borderLineUL, noiseOriginX, noiseOriginZ, _set_heights, log);
				if (blockX != CHUNK_SIZE) InitSubpatch(localX0 + blockX, localZ0 + blockZ, x0 + 4, x0 + 8, z0, z0 + 4, uc_height, ur_height, cc_height, cr_height, subpatchMixUR, noisePixels, noiseWidth, noiseHeight, nationData, blockData.nationID, borderU, borderUR, borderC, borderR, borderLineU, BorderLineType.NONE, BorderLineType.NONE, noiseOriginX, noiseOriginZ, _set_heights, log);
				if (blockZ != CHUNK_SIZE) InitSubpatch(localX0 + blockX, localZ0 + blockZ, x0, x0 + 4, z0 + 4, z0 + 8, cl_height, cc_height, dl_height, dc_height, subpatchMixDL, noisePixels, noiseWidth, noiseHeight, nationData, blockData.nationID, borderL, borderC, borderDL, borderD, BorderLineType.NONE, borderLineL, BorderLineType.NONE, noiseOriginX, noiseOriginZ, _set_heights, log);
				if ((blockX != CHUNK_SIZE) && (blockZ != CHUNK_SIZE)) InitSubpatch(localX0 + blockX, localZ0 + blockZ, x0 + 4, x0 + 8, z0 + 4, z0 + 8, cc_height, cr_height, dc_height, dr_height, subpatchMixDR, noisePixels, noiseWidth, noiseHeight, nationData, blockData.nationID, borderC, borderR, borderD, borderDR, BorderLineType.NONE, BorderLineType.NONE, BorderLineType.NONE, noiseOriginX, noiseOriginZ, _set_heights, log);

                // Instantiate AudioSource for any object in this block.
                if ((blockData.objectID != -1) && (blockX < blockX1) && (blockZ < blockZ1))
                {
                    GameObject objectAudioSource = Sound.instance.InstantiateObjectAudioSource(blockData.objectID);
                    if (objectAudioSource != null) 
                    {
                        // Position and record this object's AudioSource
                        objectAudioSource.transform.position = GetBlockCenterWorldPos(terrainX0 + blockX, terrainZ0 + blockZ);
                        terrainPatch.audioSources.Push(objectAudioSource);
                    }
                }
			}
		}

        if (_set_heights) {
		    terrainData.SetHeights(0, 0, heights);
        }

		if (singleBlockUpdate)
		{
			int updateX0 = Math.Max (0, (_singleBlockX - 1) * BLOCK_PIXEL_SIZE);
			int updateZ0 = Math.Max (0, (_singleBlockZ - 1) * BLOCK_PIXEL_SIZE);
			int updateX1 = Math.Min (ALPHA_MAP_SIZE, (_singleBlockX + 2) * BLOCK_PIXEL_SIZE);
			int updateZ1 = Math.Min (ALPHA_MAP_SIZE, (_singleBlockZ + 2) * BLOCK_PIXEL_SIZE);
			int updateSizeX = updateX1 - updateX0;
			int updateSizeZ = updateZ1 - updateZ0;
			int updateZLast = updateSizeZ - 1;

			float[,,] singleBlockAlphaMaps = new float[updateSizeZ, updateSizeX, NUM_TERRAIN_TEXTURES];
			Array.Clear (singleBlockAlphaMaps, 0, updateSizeZ * updateSizeX * NUM_TERRAIN_TEXTURES);

			for (z = 0; z < updateSizeZ; z++) {
				for (x = 0; x < updateSizeX; x++) {
					for (int t = 0; t < NUM_TERRAIN_TEXTURES; t++) {
						singleBlockAlphaMaps[updateZLast - z,x,t] = alphaMaps[ALPHA_MAP_LAST - (updateZ0 + z), updateX0 + x, t];
					}
				}
			}

			terrainData.SetAlphamaps (updateX0, ALPHA_MAP_SIZE - updateZ0 - updateSizeZ, singleBlockAlphaMaps);
		}
		else
		{
			terrainData.SetAlphamaps (0, 0, alphaMaps);
		}

		if (!singleBlockUpdate)
		{
			// Record that this terrain patch is no longer pending an update (it has just been updated).
            terrainPatch.pendingUpdate = false;
		}
	}

    bool IsNationContender(int _nationID)
    {
        if (!gameData.nationTournamentActive) {
            return false;
        }

        if (gameData.nationTable.ContainsKey(_nationID) == false) {
            return false;
        }

        return ((gameData.nationTable[_nationID].flags & (int)GameData.NationFlags.TOURNAMENT_CONTENDER) != 0);
    }

	void InitSubpatch(int _localBlockX, int _localBlockZ, int _x0, int _x1, int _z0, int _z1, float _ul_height, float _ur_height, float _dl_height, float _dr_height, float [,,] _subpatchMix, Color[] _noisePixels, int _noiseWidth, int _noiseHeight, NationData _nationData, int _nationID, float _borderUL, float _borderUR, float _borderDL, float _borderDR, BorderLineType _borderLineU, BorderLineType _borderLineL, BorderLineType _borderLineUL, int _noiseOriginX, int _noiseOriginZ, bool _set_heights, bool _log)
    {
		int absX, absZ = 0, terrain0, terrain1;
		float noiseVal, noiseVal2, height, noiseDegree, alpha, emblem_val;
		float borderDegreeZ, borderDegreeZInc, borderDegreeX, borderDegreeXInc;
        BorderLineType borderLineType;

		borderDegreeZ = 0.0f;
		borderDegreeZInc = (1.0f / (float)(_z1 - _z0));

        float gridFactor = showGrid ? ((((_localBlockX + _localBlockZ) % 2) == 0) ? 1f : 0.9f) : 1f;

		for (int z = _z0; z < _z1; z++)
		{
			absX = 0;
			borderDegreeX = borderDegreeZ * (_borderDL - _borderUL) + _borderUL;
			borderDegreeXInc = ((borderDegreeZ * (_borderDR - _borderUR) + _borderUR) - borderDegreeX) / (_x1 - _x0);

			for (int x = _x0; x < _x1; x++)
			{
				height = (_subpatchMix[absX,absZ,0] * _ul_height) + (_subpatchMix[absX,absZ,1] * _ur_height) + (_subpatchMix[absX,absZ,2] * _dl_height) + (_subpatchMix[absX,absZ,3] * _dr_height);

				// Get red value of corresponding location in greycale noise texture.
				noiseVal = _noisePixels[((x + _noiseOriginX) & NOISE_TEXTURE_SIZE_MASK) + (((z + _noiseOriginZ) & NOISE_TEXTURE_SIZE_MASK) * NOISE_TEXTURE_WIDTH)].r;
				noiseVal2 = _noisePixels[(((x + _noiseOriginX) << 3) & NOISE_TEXTURE_SIZE_MASK) + ((((z + _noiseOriginZ) << 3) & NOISE_TEXTURE_SIZE_MASK) * NOISE_TEXTURE_WIDTH)].r;
               
                // Determine the degree to which noise may be applied to vary the height here.
				if (height >= 0.55) {
                    // Continuous increase from 0 at flat land height, up.
					noiseDegree = (height - 0.55f) * 1.5f;
				} else if (height >= 0.52) {
                    // Continuous decrease from 0.1 at beach height, through 0 at flat land height.
                    noiseDegree = ((0.55f - height) / 0.03f) * 0.1f;
				} else {
                    // Continuous decrease to 0.1 at beach height.
					noiseDegree = (0.52f - height) + 0.1f;
				}
				noiseDegree += 0.025f;
                
				// Coarse noise
				height = height + (noiseVal - 0.5f) * noiseDegree;

				// Fine noise
				if (height > 0.6f) {
					height += (noiseVal2 - 0.5f) * 0.1f;
				}
                
                if (_set_heights) {
				    heights[HEIGHT_MAP_LAST - z, x] = height;
                }

				if (_log) {
					Debug.Log("InitSubpatch() x: " + x + ", z: " + z + ", height: " + height);
				}

				//if (!_log) heights[HEIGHT_MAP_LAST - z,x] = 0.0f; // TESTING

				if (_nationID == -1) {
					borderDegreeX = 0f;
				}

				if ((x < ALPHA_MAP_SIZE) && (z < ALPHA_MAP_SIZE)) {
					//alphaMaps[ALPHA_MAP_LAST - z,x,8] = 1.0f;
					//alphaMaps[ALPHA_MAP_LAST - z,x,7] = ((((_x0 / 8) + (_z0 / 8)) % 2) == 0) ? 1.0f : 0.0f;
					//alphaMaps[ALPHA_MAP_LAST - z,x,9] = 1.0f;

					if (height < alphaMapCenterHeights[0]) {
						terrain0 = 0;
						terrain1 = 1;
						alpha = 0.0f;
					} else if (height < alphaMapCenterHeights[1]) {
						terrain0 = 0;
						terrain1 = 1;
						alpha = (height - alphaMapCenterHeights[0]) / (alphaMapCenterHeights[1] - alphaMapCenterHeights[0]);
					} else if (height < alphaMapCenterHeights[2]) {
						terrain0 = 1;
						terrain1 = 2;
						alpha = (height - alphaMapCenterHeights[1]) / (alphaMapCenterHeights[2] - alphaMapCenterHeights[1]);
					} else if (height < alphaMapCenterHeights[3]) {
						terrain0 = 2;
						terrain1 = 3;
						alpha = (height - alphaMapCenterHeights[2]) / (alphaMapCenterHeights[3] - alphaMapCenterHeights[2]);
					} else if (height < alphaMapCenterHeights[4]) {
						terrain0 = 3;
						terrain1 = 4;
						alpha = (height - alphaMapCenterHeights[3]) / (alphaMapCenterHeights[4] - alphaMapCenterHeights[3]);
					} else if (height < alphaMapCenterHeights[5]) {
						terrain0 = 3;
						terrain1 = 4;
						alpha = 1.0f;
					} else if (height < alphaMapCenterHeights[6]) {
						terrain0 = 4;
						terrain1 = 5;
						alpha = (height - alphaMapCenterHeights[4]) / (alphaMapCenterHeights[5] - alphaMapCenterHeights[4]);
					} else {
						terrain0 = 4;
						terrain1 = 5;
						alpha = 1.0f;
					}

					alpha += ((noiseVal - 0.5f) * 4.0f);
					alpha = Math.Max (alpha, 0.0f);
					alpha = Math.Min (alpha, 1.0f);

					// TESTING
					//if ((x % 16) == 0)
					//{
					//	alphaMaps[ALPHA_MAP_LAST - z,x,9] = 1.0f;
					//} else 

                    // Determine the border type, if at a nation border.
                    if ((_borderLineUL != BorderLineType.NONE) && (x == _x0) && (z == _z0)) {
                        borderLineType = _borderLineUL;
                    } else if ((_borderLineU != BorderLineType.NONE) && (z == _z0)) {
                        borderLineType = _borderLineU;
                    } else if ((_borderLineL != BorderLineType.NONE) && (x == _x0)) {
                        borderLineType = _borderLineL;
                    } else {
                        borderLineType = BorderLineType.NONE;
                    }
                    
					if (borderLineType == BorderLineType.NATION_BORDER)
					{
                        // Display the border line at the edge of a nation.
						alphaMaps[ALPHA_MAP_LAST - z,x,6] = NATION_BORDER_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,7] = NATION_BORDER_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,8] = NATION_BORDER_VALUE;
					}
                    else if (borderLineType == BorderLineType.CONTENDER_NATION_BORDER)
					{
                        // Display the border line at the edge of another tournament contender's nation.
						alphaMaps[ALPHA_MAP_LAST - z,x,6] = CONTENDER_NATION_BORDER_RED_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,7] = CONTENDER_NATION_BORDER_GREEN_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,8] = CONTENDER_NATION_BORDER_BLUE_VALUE;
					}
					else if (borderLineType == BorderLineType.PLAYER_NATION_BORDER)
					{
                        // Display the border line at the edge of the player's nation.
						alphaMaps[ALPHA_MAP_LAST - z,x,6] = PLAYER_NATION_BORDER_RED_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,7] = PLAYER_NATION_BORDER_GREEN_VALUE;
						alphaMaps[ALPHA_MAP_LAST - z,x,8] = PLAYER_NATION_BORDER_BLUE_VALUE;
					}
					else
					{
						//if (_nationData != null) alphaMaps[ALPHA_MAP_LAST - z,x,8] = 1.0f; // TESTING
						//else alphaMaps[ALPHA_MAP_LAST - z,x,terrain0] = 1.0f; // TESTING

						alphaMaps[ALPHA_MAP_LAST - z,x,terrain0] = (1.0f - alpha) * (1.0f - borderDegreeX) * gridFactor;
						alphaMaps[ALPHA_MAP_LAST - z,x,terrain1] = alpha * (1.0f - borderDegreeX) * gridFactor;

						if (_nationData != null)
						{
                            if (_nationData.emblem_u == -1)
                            {
							    alphaMaps[ALPHA_MAP_LAST - z,x,6] = _nationData.r * borderDegreeX * gridFactor;
							    alphaMaps[ALPHA_MAP_LAST - z,x,7] = _nationData.g * borderDegreeX * gridFactor;
							    alphaMaps[ALPHA_MAP_LAST - z,x,8] = _nationData.b * borderDegreeX * gridFactor;
                            }
                            else
                            {
                                emblem_val = emblemData[_nationData.emblem_u + (x & EMBLEM_DIM_BITMASK), _nationData.emblem_v + (z & EMBLEM_DIM_BITMASK)];

                                switch (_nationData.emblem_color)
                                {
                                    case NationData.EmblemColor.WHITE:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = (1f - ((1f - _nationData.r) * emblem_val)) * borderDegreeX * gridFactor;
							            alphaMaps[ALPHA_MAP_LAST - z,x,7] = (1f - ((1f - _nationData.g) * emblem_val)) * borderDegreeX * gridFactor;
							            alphaMaps[ALPHA_MAP_LAST - z,x,8] = (1f - ((1f - _nationData.b) * emblem_val)) * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.BLACK:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = _nationData.r * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,7] = _nationData.g * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,8] = _nationData.b * emblem_val * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.RED:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = (1f - ((1f - _nationData.r) * emblem_val)) * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,7] = _nationData.g * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,8] = _nationData.b * emblem_val * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.YELLOW:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = (1f - ((1f - _nationData.r) * emblem_val)) * borderDegreeX * gridFactor;
							            alphaMaps[ALPHA_MAP_LAST - z,x,7] = (1f - ((1f - _nationData.g) * emblem_val)) * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,8] = _nationData.b * emblem_val * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.GREEN:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = _nationData.r * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,7] = (1f - ((1f - _nationData.g) * emblem_val)) * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,8] = _nationData.b * emblem_val * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.BLUE:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = _nationData.r * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,7] = _nationData.g * emblem_val * borderDegreeX * gridFactor;
                                        alphaMaps[ALPHA_MAP_LAST - z,x,8] = (1f - ((1f - _nationData.b) * emblem_val)) * borderDegreeX * gridFactor;
                                        break;
                                    case NationData.EmblemColor.PURPLE:
                                    default:
                                        alphaMaps[ALPHA_MAP_LAST - z,x,6] = (1f - ((1f - _nationData.r) * emblem_val)) * borderDegreeX * gridFactor;
							            alphaMaps[ALPHA_MAP_LAST - z,x,7] = _nationData.g * emblem_val * borderDegreeX * gridFactor;
							            alphaMaps[ALPHA_MAP_LAST - z,x,8] = (1f - ((1f - _nationData.b) * emblem_val)) * borderDegreeX * gridFactor;
                                        break;
                                }
                            }
						}
					}
				}

				if (x == HEIGHT_MAP_LAST) {
					break;
				}

				absX++;
				borderDegreeX += borderDegreeXInc;
			}

			if (z == HEIGHT_MAP_LAST) {
				break;
			}

			absZ++;
			borderDegreeZ += borderDegreeZInc;
		}
	}

	void InitSubpatchMix(float[,,] _subpatchMix, Vector2 _pos_ul, Vector2 _pos_ur, Vector2 _pos_dl, Vector2 _pos_dr)
	{
		float influence_ul, influence_dl, influence_ur, influence_dr, total_influence;
		
		for (int z = 0; z < 4; z++) {
			for (int x = 0; x < 4; x++) {
				/*
				influence_ul = (float)Math.Pow(1.0f - (Math.Max(Math.Abs((float)x - _pos_ul.x), Math.Abs((float)z - _pos_ul.y)) / 8.0), TERRAIN_SMOOTHING_POWER);
				influence_ur = (float)Math.Pow(1.0f - (Math.Max(Math.Abs((float)x - _pos_ur.x), Math.Abs((float)z - _pos_ur.y)) / 8.0), TERRAIN_SMOOTHING_POWER);
				influence_dl = (float)Math.Pow(1.0f - (Math.Max(Math.Abs((float)x - _pos_dl.x), Math.Abs((float)z - _pos_dl.y)) / 8.0), TERRAIN_SMOOTHING_POWER);
				influence_dr = (float)Math.Pow(1.0f - (Math.Max(Math.Abs((float)x - _pos_dr.x), Math.Abs((float)z - _pos_dr.y)) / 8.0), TERRAIN_SMOOTHING_POWER);
*/
				influence_ul = (float)Math.Pow((1.0 - (Math.Min(Math.Pow(Math.Pow(((double)x - _pos_ul.x), 2.0) + Math.Pow(((double)z - _pos_ul.y), 2.0), 0.5), 8.0) / 8.0)), TERRAIN_SMOOTHING_POWER);
				influence_ur = (float)Math.Pow((1.0 - (Math.Min(Math.Pow(Math.Pow(((double)x - _pos_ur.x), 2.0) + Math.Pow(((double)z - _pos_ur.y), 2.0), 0.5), 8.0) / 8.0)), TERRAIN_SMOOTHING_POWER);
				influence_dl = (float)Math.Pow((1.0 - (Math.Min(Math.Pow(Math.Pow(((double)x - _pos_dl.x), 2.0) + Math.Pow(((double)z - _pos_dl.y), 2.0), 0.5), 8.0) / 8.0)), TERRAIN_SMOOTHING_POWER);
				influence_dr = (float)Math.Pow((1.0 - (Math.Min(Math.Pow(Math.Pow(((double)x - _pos_dr.x), 2.0) + Math.Pow(((double)z - _pos_dr.y), 2.0), 0.5), 8.0) / 8.0)), TERRAIN_SMOOTHING_POWER);

				total_influence = influence_ul + influence_ur + influence_dl + influence_dr;
				_subpatchMix[(int)x,z,0] = influence_ul / total_influence;
				_subpatchMix[x,z,1] = influence_ur / total_influence;
				_subpatchMix[x,z,2] = influence_dl / total_influence;
				_subpatchMix[x,z,3] = influence_dr / total_influence;
				
				//if ((_pos_ul.x == 0.0f) && (_pos_ul.y == 0.0f)) Debug.Log (x +"," + z + ": " + _subpatchMix[x,z,0] + "," + _subpatchMix[x,z,1] + "," + _subpatchMix[x,z,2] + "," + _subpatchMix[x,z,3]);		
			}
		}
	}

    public int GetBlockTerrain(int _block_x, int _block_z)
    {
        int result;

        if (mapDimX == Int32.MinValue)
        {
            Debug.Log("ERROR: GetBlockTerrain() called before map dimensions have been received.");
            return TERRAIN_DEEP_WATER;
        }

        if (_block_x < 0)
        {
            int borderTerrain = GetBlockTerrain(0, Math.Min(Math.Max(_block_z, 0), mapDimZ - 1));
            int distFromBorderZ = (_block_z < 0) ? -_block_z : ((_block_z >= mapDimZ) ? (_block_z - (mapDimZ - 1)) : 0);
            int distFromBorderX = -_block_x;
            int distFromBorder = Math.Max(distFromBorderX, distFromBorderZ) / 3 + 1;
            result = (borderTerrain == TERRAIN_BEACH) ? TERRAIN_BEACH : ((borderTerrain <= TERRAIN_SHALLOW_WATER) ? (Math.Max(borderTerrain - distFromBorder, TERRAIN_DEEP_WATER)) : Math.Min(borderTerrain + distFromBorder, TERRAIN_TALL_MOUNTAINS));
        }
        else if (_block_x >= mapDimX)
        {
            // Note: stack overflow error here may mean that this is being called before mapDimX is set!
            int borderTerrain = GetBlockTerrain(mapDimX - 1, Math.Min(Math.Max(_block_z, 0), mapDimZ - 1));
            int distFromBorderZ = (_block_z < 0) ? -_block_z : ((_block_z >= mapDimZ) ? (_block_z - (mapDimZ - 1)) : 0);
            int distFromBorderX = _block_x - (mapDimX - 1);
            int distFromBorder = Math.Max(distFromBorderX, distFromBorderZ) / 3 + 1;
            result = (borderTerrain == TERRAIN_BEACH) ? TERRAIN_BEACH : ((borderTerrain <= TERRAIN_SHALLOW_WATER) ? (Math.Max(borderTerrain - distFromBorder, TERRAIN_DEEP_WATER)) : Math.Min(borderTerrain + distFromBorder, TERRAIN_TALL_MOUNTAINS));
        }
        else if (_block_z < 0)
        {
            int borderTerrain = GetBlockTerrain(Math.Min(Math.Max(_block_x, 0), mapDimX - 1), 0);
            int distFromBorderX = (_block_x < 0) ? -_block_x : ((_block_x >= mapDimX) ? (_block_x - (mapDimX - 1)) : 0);
            int distFromBorderZ = -_block_z;
            int distFromBorder = Math.Max(distFromBorderX, distFromBorderZ) / 3 + 1;
            result = (borderTerrain == TERRAIN_BEACH) ? TERRAIN_BEACH : ((borderTerrain <= TERRAIN_SHALLOW_WATER) ? (Math.Max(borderTerrain - distFromBorder, TERRAIN_DEEP_WATER)) : Math.Min(borderTerrain + distFromBorder, TERRAIN_TALL_MOUNTAINS));
        }
        else if (_block_z >= mapDimZ)
        {
            int borderTerrain = GetBlockTerrain(Math.Min(Math.Max(_block_x, 0), mapDimX - 1), mapDimZ - 1);
            int distFromBorderX = (_block_x < 0) ? -_block_x : ((_block_x >= mapDimX) ? (_block_x - (mapDimX - 1)) : 0);
            int distFromBorderZ = _block_z - (mapDimZ - 1);
            int distFromBorder = Math.Max(distFromBorderX, distFromBorderZ) / 3 + 1;
            result = (borderTerrain == TERRAIN_BEACH) ? TERRAIN_BEACH : ((borderTerrain <= TERRAIN_SHALLOW_WATER) ? (Math.Max(borderTerrain - distFromBorder, TERRAIN_DEEP_WATER)) : Math.Min(borderTerrain + distFromBorder, TERRAIN_TALL_MOUNTAINS));
        }
        else 
        {
            result = mapTerrain[_block_x, _block_z];
        }

        // Sanity check result.
        if ((result < 0) || (result >= TERRAIN_NUM_TYPES)) 
        {
            Debug.Log("ERROR: GetBlockTerrain(" + _block_x + ", " + _block_z + ") has out of bounds result of " + result + ".");
            result = TERRAIN_DEEP_WATER;
        }

        return result;
    }

    public bool IsBlockAdjacentToNation(int _blockX, int _blockZ)
    {
        BlockData block_data;

        // If any block adjacent to the given block belongs to this player's nation, return true.
        block_data = GetBlockData(_blockX - 1, _blockZ - 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX, _blockZ - 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX + 1, _blockZ - 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX - 1, _blockZ);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX + 1, _blockZ);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX - 1, _blockZ + 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX, _blockZ + 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;
        block_data = GetBlockData(_blockX + 1, _blockZ + 1);
        if ((block_data != null) && (block_data.nationID == gameData.nationID)) return true;

        return false;
    }

    // Return true if a block with the given _nationID is within _radius of he given position.
    public bool NationIsInArea(int _nationID, int _x, int _z, int _radius)
    {
        BlockData block_data;

        for (int z = (_z - _radius); z <= (_z + _radius); z++)
        {
            for (int x = (_x - _radius); x <= (_x + _radius); x++)
            {
                block_data = GetBlockData(x, z);

                if ((block_data != null) && (block_data.nationID == _nationID)) {
                    return true;
                }
            }
        }

        return false;
    }

    public int GetViewBlockX()
    {
        return viewBlockX;
    }

    public int GetViewBlockZ()
    {
        return viewBlockZ;
    }

    public float GetBlockHeight(int _blockX, int _blockZ)
    {
        if ((GetBlockTerrain(_blockX, _blockZ) < 0) || (GetBlockTerrain(_blockX, _blockZ) >= TERRAIN_NUM_TYPES)) Debug.Log("Block " + _blockX + "," + _blockZ + " block terrain " + GetBlockTerrain(_blockX, _blockZ));
        return terrainHeights[GetBlockTerrain(_blockX, _blockZ)] * TERRAIN_PATCH_HEIGHT;
	}

    public Vector3 GetBlockCenterWorldPos(int _blockX, int _blockZ)
    {
        float height = GetBlockHeight(_blockX, _blockZ);
        return new Vector3((((float)_blockX) + 0.5f) * BLOCK_SIZE, height, -(((float)_blockZ) + 0.5f) * BLOCK_SIZE);
    }

    public Vector3 GetBlockCenterWorldPosFlat(int _blockX, int _blockZ)
    {
        return new Vector3((((float)_blockX) + 0.5f) * BLOCK_SIZE, LAND_LEVEL, -(((float)_blockZ) + 0.5f) * BLOCK_SIZE);
    }

    public Vector3 GetBlockCenterScreenPos(int _blockX, int _blockZ)
    {
        float height = GetBlockHeight(_blockX, _blockZ);

        Vector3 world_coords = new Vector3((((float)_blockX) + 0.5f) * BLOCK_SIZE, height, -(((float)_blockZ) + 0.5f) * BLOCK_SIZE);
        return camera.WorldToScreenPoint(world_coords);
    }

    private IntVector2 WorldPointToWorldBlock(Vector3 _world_point)
    {
        IntVector2 world_block = new IntVector2((int)(_world_point.x / BLOCK_SIZE) - ((_world_point.x < 0f) ? 1 : 0),
                                                (int)(-_world_point.z / BLOCK_SIZE) - (((-_world_point.z) < 0f) ? 1 : 0));
        return world_block;
    }

    IntVector2 WorldBlockToLocalBlock(IntVector2 _world_block)
    {
        IntVector2 local_block = new IntVector2(_world_block.x - viewDataBlockX0, _world_block.z - viewDataBlockZ0);
        return local_block;
    }

    IntVector2 WorldBlockToWorldChunk(IntVector2 _world_block)
	{
		IntVector2 world_chunk = new IntVector2(((_world_block.x < 0) ? (_world_block.x - CHUNK_SIZE_MINUS_1) : _world_block.x) / CHUNK_SIZE, 
                                                ((_world_block.z < 0) ? (_world_block.z - CHUNK_SIZE_MINUS_1) : _world_block.z) / CHUNK_SIZE);
		return world_chunk;
	}

    IntVector2 WorldChunkToPatch(IntVector2 _world_chunk)
	{
        IntVector2 patch = new IntVector2(_world_chunk.x - viewChunkX0, _world_chunk.z - viewChunkZ0);

		// If the given chunk does not correspond to a terrain patch in the current view, set the coords to -1.
		if ((patch.x < 0) || (patch.x >= VIEW_DATA_CHUNKS_WIDE) || (patch.z < 0) || (patch.z >= VIEW_DATA_CHUNKS_WIDE)) {
			patch.x = -1;
			patch.z = -1;
		}

		return patch;
	}

    int WorldBlockToLocalChunkX(int _x)
    {
        //return (_x / CHUNK_SIZE) - ((_x < 0f) ? 1 : 0) - viewChunkX0;
        return (((_x < 0) ? (_x - CHUNK_SIZE_MINUS_1) : _x) / CHUNK_SIZE) - viewChunkX0;
    }

    int WorldBlockToLocalChunkZ(int _z)
    {
        //return (_z / CHUNK_SIZE) - ((_z < 0f) ? 1 : 0) - viewChunkZ0;
        return (((_z < 0) ? (_z - CHUNK_SIZE_MINUS_1) : _z) / CHUNK_SIZE) - viewChunkZ0;
    }

    int WorldBlockToGlobalChunkX(int _x)
    {
        return ((_x < 0) ? (_x - CHUNK_SIZE_MINUS_1) : _x) / CHUNK_SIZE;
    }

    int WorldBlockToGlobalChunkZ(int _z)
    {
        return ((_z < 0) ? (_z - CHUNK_SIZE_MINUS_1) : _z) / CHUNK_SIZE;
    }

    public void DeterminePosition()
	{
        float leftOffset = GameGUI.instance.GetMainUILeftWidth() * canvas.scaleFactor;
        float bottomOffset = Math.Min(Screen.height - 1, (GameGUI.instance.GetMainUIBottomHeight() + Chat.instance.GetOpaqueChatHeight()) * canvas.scaleFactor);

		// Determine fraction of screen that left and bottom UIs take up.
		float mainUILeftFraction = leftOffset / Screen.width;
		float mainUIBottomFraction = bottomOffset / Screen.height;

		// Determine fraction of screen that 3D viewport should take up.
		float cameraRectWidth = 1f - mainUILeftFraction;
		float cameraRectHeight = 1f - mainUIBottomFraction;

		// Repositon and resize 3D viewport.
		camera.rect = new Rect(mainUILeftFraction, mainUIBottomFraction, cameraRectWidth, cameraRectHeight);
        overlayCamera.rect = camera.rect;

        //Debug.Log("DETERMINEPOSITION(): mainUILeftFraction: " + mainUILeftFraction + ", mainUIBottomFraction: " + mainUIBottomFraction + ", cameraRectWidth: " + cameraRectWidth + ", cameraRectHeight: " + cameraRectHeight);

		// Do not allow either vertical or horizontal field of view angle to exceed maximums.
		if ((cameraRectHeight * Screen.height) >= (cameraRectWidth * Screen.width)) {
			camera.fieldOfView = MAX_VERTICAL_CAMERA_FIELD_OF_VIEW + (MAX_VERTICAL_CAMERA_FIELD_OF_VIEW * (((cameraRectHeight * Screen.height) / (cameraRectWidth * Screen.width) - 1f) * 0.6f));
		} else {
			camera.fieldOfView = MAX_VERTICAL_CAMERA_FIELD_OF_VIEW;
		}

        overlayCamera.fieldOfView = camera.fieldOfView;

        // Update the MapView GUI for this change in view.
        UpdateMapGUI();

        // Update the view area.
        UpdateViewArea();
    }

    public void DisplayNewAttack(int _duration, int _x, int _z, int _attack_points_max, int _attack_points_start, int _attack_points_end, int _defend_points_max, int _defend_points_start, int _defend_points_end, int _attacker_stat, int _defender_stat, int _attacker_nation_ID, int _defender_nation_ID, int _effect_techID, int _battle_flags)
    {
        DisplayAttack.ActivateNew(_duration, _x, _z, _attack_points_max, _attack_points_start, _attack_points_end, _defend_points_max, _defend_points_start, _defend_points_end, _attacker_stat, _defender_stat, _attacker_nation_ID, _defender_nation_ID, _battle_flags);

        // Tell the tutorial system about this battle.
        Tutorial.instance.BattleBegins(_duration, _defend_points_end == 0, _defender_nation_ID);

        // If a discovery is about to take place, display icon.
        if ((_battle_flags & (int)GameData.BattleFlags.DISCOVERY) != 0) {
            DisplayDiscoveryIcon(_x, _z, 3.5f);
        }

        if (_effect_techID != -1)
        {
            if (_effect_techID == 306) // Radioactive Fallout
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.radioactiveFalloutFX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
            }
            else if (_effect_techID == 360) // Contagion
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.contagionFX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
            }
            else if (_effect_techID == 409) // Psychic Shockwave
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.psychicShockwaveFX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
            }
            else if (_effect_techID == 465) // Guerrilla Warfare
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.guerrillaWarfareFX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
            }
            else if (_effect_techID == 515) // Battle Plague
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.battlePlagueFX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
            }
        }
    }

    public void DisplayNewProcess(int _duration, int _x, int _z, int _hit_points_max, int _hit_points_start, int _hit_points_end, int _process_flags)
    {
        DisplayProcess.ActivateNew(_duration, _x, _z, _hit_points_max, _hit_points_start, _hit_points_end);

        // If a discovery is about to take place, display icon.
        if ((_process_flags & (int)GameData.ProcessFlags.DISCOVERY) != 0) {
            DisplayDiscoveryIcon(_x, _z, 0.5f);
        }
    }

    public void DisplayDiscoveryIcon(int _x, int _z, float _delay)
    {
        StartCoroutine(DisplayDiscoveryIcon_Coroutine(_x, _z, _delay));
    }

    public IEnumerator DisplayDiscoveryIcon_Coroutine(int _x, int _z, float _delay)
    {
        yield return new WaitForSeconds(_delay);

        GameObject discoveryIconObject = ((GameObject) Instantiate(BuildPrefabs.instance.discoveryIcon, GetBlockCenterWorldPos(_x, _z) + new Vector3(0,40,0), Quaternion.Euler(0, 45, 0)));
        GameObject.Destroy(discoveryIconObject, 3f);
    }

    public void InitBlockProcess(int _x, int _z, int _process_type, int _nationID, float _delay, float _hit_points_start, float _hit_points_full, float _hit_points_rate, int _battle_flags = 0)
    {
        // Cancel any previous GUI processes for the given block, before starting the new process.
        DisplayAttack.CancelForBlock(_x, _z);
        DisplayProcess.CancelForBlock(_x, _z);
        DisplayHitPoints.CancelForBlock(_x, _z);
        BlockProcess.CancelForBlock(_x, _z);

        // Get the data for this block.
        BlockData block_data = GetBlockData(_x, _z);

        // Do nothing if this client is no longer viewing the given block.
        if (block_data == null) {
            return;
        }

        // If this process represents an attack on the block...
        if (_process_type == BlockProcess.PROCESS_BATTLE)
        {
            // Have any object in the attacked block become visible.
            UpdateObjectInvisibilityForAttack(_x, _z);
        }

        // Activate the new block process.
        BlockProcess.ActivateNew(_x, _z, _process_type, _nationID, _delay, _hit_points_start, _hit_points_full, _hit_points_full, _hit_points_rate, _battle_flags);

        // If this is an occupation or evacuation by this nation...
        if (((_process_type == BlockProcess.PROCESS_OCCUPY) && (_nationID == GameData.instance.nationID)) ||
            ((_process_type == BlockProcess.PROCESS_EVACUATE) && (block_data.nationID == GameData.instance.nationID)))
        {
            // Tell the tutorial system about this block process.
            Tutorial.instance.BlockProcessBegins(_delay, _process_type);
        }

        if ((_process_type == BlockProcess.PROCESS_OCCUPY) && (_nationID == GameData.instance.nationID)) 
        {
            // Tell the sound system that the nation has occupied a block
            Sound.instance.BlockOccupied(_x, _z, block_data);
        }
    }

    public void InitBattleProcess(int _x, int _z, int _nationID, float _delay, float _battle_duration, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _hit_points_new_cur, float _hit_points_new_full, float _hit_points_rate, int _initiatorUserID, int _battle_flags)
    {
        // Cancel any previous GUI processes for the given block, before starting the new process.
        DisplayAttack.CancelForBlock(_x, _z);
        DisplayProcess.CancelForBlock(_x, _z);
        DisplayHitPoints.CancelForBlock(_x, _z);
        BlockProcess.CancelForBlock(_x, _z);

        // Get the data for this block.
        BlockData block_data = GetBlockData(_x, _z);

        // Do nothing if this client is no longer viewing the given block.
        if (block_data == null) {
            return;
        }

        // Have any object in the attacked block become visible.
        UpdateObjectInvisibilityForAttack(_x, _z);

        if (_initiatorUserID != GameData.instance.userID)
        {
            // After any given _delay, display the hit points decreasing, for damage being taken.
            StartCoroutine(DelayedDisplayHitPoints(_x, _z, _hit_points_start, _hit_points_end, _hit_points_full, _battle_duration, DisplayHitPoints.TransitionType.RANDOM, false, true, _delay));
        }

        if (block_data.nationID != _nationID) {
            GameData.instance.BlockCaptured(_x, _z, _nationID, block_data.nationID, block_data, _delay + _battle_duration);
        }

        // Activate the  block process to display restoring of hit points.
        BlockProcess.ActivateNew(_x, _z, BlockProcess.PROCESS_BATTLE, _nationID, _battle_duration, _hit_points_new_cur, _hit_points_new_full, _hit_points_new_full, _hit_points_rate, _battle_flags);
    }

    public void InitTowerAction(int _x, int _z, int _build_ID, BuildData.Type _build_type, float _invisible_time, int _duration, int _trigger_x, int _trigger_z, int _triggerNationID, List<TargetRecord> _targets)
    {
        StartCoroutine(ManageTowerAction(_x, _z, _build_ID, _build_type, _invisible_time, _duration, _trigger_x, _trigger_z, _triggerNationID, _targets));
    }

    public IEnumerator ManageTowerAction(int _x, int _z, int _build_ID, BuildData.Type _build_type, float _invisible_time, int _duration, int _trigger_x, int _trigger_z, int _triggerNationID, List<TargetRecord> _targets)
    {
        Debug.Log("ManageTowerAction() begins at time " + Time.time + ", _build_type: " + _build_type);

        float start_action_time = Time.time;

        BlockData block_data = GetBlockData(_x, _z);

        // Do nothing if this client is no longer viewing the given block.
        if (block_data == null) {
            yield break;
        }

        // Record the user event that a defense has been triggered by or of the player's nation.
        if ((block_data.nationID == GameData.instance.nationID) || (_triggerNationID == GameData.instance.nationID)) {
            GameData.instance.DefenseTriggered(_triggerNationID, block_data.nationID);
        }

        // Get the build's data
        BuildData build_data = BuildData.GetBuildData(_build_ID);

        if (build_data.cooldown_time > 0)
        {
            // Activate a DisplayTimer to 
            StartCoroutine(DelayedDisplayTimer(_x, _z, DisplayTimer.Type.ACTIVATE, Time.time + _duration, Time.time + _duration + build_data.cooldown_time, _duration));
        }

        // Record the block's invisible_time, and update the block's build object.
        float local_invisible_time = (_invisible_time == -1f) ? -1f : (_invisible_time + start_action_time);
        block_data.invisible_time = local_invisible_time;
        if (block_data.build_object != null) {
            block_data.build_object.UpdateInvisibility();
        }

        if (_build_type == BuildData.Type.DIRECTED_MULTIPLE)
        {
            TargetRecord cur_target;
            int num_targets = _targets.Count;
            float time_per_target = ((float)_duration) / num_targets;
            //float damage_duration = Mathf.Min(time_per_target, DAMAGE_DURATION);

            for (int target_index = 0; target_index < num_targets; target_index++)
	        {
                cur_target = _targets[target_index];

                // Record the block's new target block.
                block_data.target_x = cur_target.x;
                block_data.target_z = cur_target.y;

                // If the block's build object is showing, change the object's aim to point toward the target block.
                if (block_data.build_object != null) {
                    block_data.build_object.Aim(cur_target.x, cur_target.y, time_per_target * DIRECTED_MULTIPLE_AIM_END_TIME);
                }

                yield return new WaitForSeconds(time_per_target * DIRECTED_MULTIPLE_AIM_END_TIME);

                // Have the object fire.
                if (block_data.build_object != null) {
                    block_data.build_object.Fire(cur_target.x, cur_target.y, (time_per_target * (1f - DIRECTED_MULTIPLE_AIM_END_TIME)));
                }

                // Have any object in the attacked block become visible.
                UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                // After delay of DIRECTED_MULTIPLE_FIRE_DURATION, display the hit points decreasing, for damage being taken.
                StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, DIRECTED_MULTIPLE_FIRE_DURATION));

                // If the next target is not this same block, then set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                if ((target_index == (num_targets - 1)) || (cur_target.x != _targets[target_index + 1].x) || (cur_target.y != _targets[target_index + 1].y)) {
	                BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, DIRECTED_MULTIPLE_FIRE_DURATION + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                }

                yield return new WaitForSeconds((time_per_target * (1f - DIRECTED_MULTIPLE_AIM_END_TIME)));
	        }
        }
        else if (_build_type == BuildData.Type.RECAPTURE)
        {
            TargetRecord cur_target;
            int num_targets = _targets.Count;
            float time_per_target = ((float)_duration) / num_targets;
            //float damage_duration = Mathf.Min(time_per_target, DAMAGE_DURATION);

            bool ecto_ray = build_data.original_name.Contains("Ecto Ray");
            bool djinn_portal = build_data.original_name.Contains("Djinn Portal");

            if (djinn_portal)
            {
                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.djinni_appear, MapView.instance.GetBlockCenterWorldPos(_x, _z));

                if (block_data.build_object != null) {
                    block_data.build_object.ShowPortal(true);
                }

                yield return new WaitForSeconds(0.5f);

                if (block_data.build_object != null) {
                    block_data.build_object.ShowGenie(true);
                }

                yield return new WaitForSeconds(1f);
            }

            float fire_duration = djinn_portal ? DJINN_PORTAL_FIRE_DURATION : ECTO_RAY_FIRE_DURATION;

            for (int target_index = 0; target_index < num_targets; target_index++)
	        {
                cur_target = _targets[target_index];

                // Record the block's new target block.
                block_data.target_x = cur_target.x;
                block_data.target_z = cur_target.y;

                // If the block's build object is showing, change the object's aim to point toward the target block.
                if (block_data.build_object != null) {
                    block_data.build_object.Aim(cur_target.x, cur_target.y, time_per_target * DIRECTED_MULTIPLE_AIM_END_TIME);
                }

                yield return new WaitForSeconds(time_per_target * DIRECTED_MULTIPLE_AIM_END_TIME);

                // Have the object fire.
                if (block_data.build_object != null) {
                    block_data.build_object.Fire(cur_target.x, cur_target.y, (time_per_target * (1f - DIRECTED_MULTIPLE_AIM_END_TIME)));
                }

                // Have any object in the attacked block become visible.
                UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                // After delay of DIRECTED_MULTIPLE_FIRE_DURATION, display the hit points decreasing, for damage being taken.
                StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, fire_duration));
                
                // If the next target is not this same block, then set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                if ((target_index == (num_targets - 1)) || (cur_target.x != _targets[target_index + 1].x) || (cur_target.y != _targets[target_index + 1].y)) {
	                BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, fire_duration + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                }

                yield return new WaitForSeconds((time_per_target * (1f - DIRECTED_MULTIPLE_AIM_END_TIME)));
	        }

            if (djinn_portal)
            {
                if (block_data.build_object != null) {
                    block_data.build_object.ShowGenie(false);
                }

                yield return new WaitForSeconds(0.5f);

                if (block_data.build_object != null) {
                    block_data.build_object.ShowPortal(false);
                }
            }
        }
        else if (_build_type == BuildData.Type.SPLASH)
        {
            TargetRecord cur_target;
            int num_targets = _targets.Count;
            float time_per_target = ((float)_duration) / num_targets;
            int target_x = 0, target_z = 0;
            
            // Find the block that is the central target for this splash.
            for (int target_index = 0; target_index < num_targets; target_index++)
	        {
                cur_target = _targets[target_index];
                if ((cur_target.battle_flags & (int)GameData.BattleFlags.TARGET_BLOCK) != 0)
                {
                    target_x = cur_target.x;
                    target_z = cur_target.y;
                }
            }

            // Record the block's new target block.
            block_data.target_x = target_x;
            block_data.target_z = target_z;

            // If the block's build object is showing, change the object's aim to point toward the target block.
            if (block_data.build_object != null) {
                block_data.build_object.Aim(target_x, target_z, SPLASH_AIM_DURATION - 0.1f);
            }

            yield return new WaitForSeconds(SPLASH_AIM_DURATION);

            // Have the object fire.
            if (block_data.build_object != null) {
                block_data.build_object.Fire(target_x, target_z, SPLASH_FIRE_DURATION);
            }

            yield return new WaitForSeconds(SPLASH_FIRE_DURATION);

            float splash_duration = _duration - SPLASH_AIM_DURATION - SPLASH_FIRE_DURATION;
            float splash_start_time = Time.time;
            float cur_splash_radius, cur_splash_radius_squared, prev_splash_radius_squared = 0, cur_target_dist_squared;

            // Loop to spread splash damage over time.
            for (;;)
            {
                // Determine the radius of the splash for this current update.
                cur_splash_radius = (Time.time - splash_start_time) * (float)build_data.effect_radius / splash_duration + 0.5f;
                cur_splash_radius_squared = cur_splash_radius * cur_splash_radius;

                // Iterate targets, and apply damage to those that have just fallen within the splash radius with this current update.
                for (int target_index = 0; target_index < num_targets; target_index++)
	            {
                    cur_target = _targets[target_index];
                    cur_target_dist_squared = ((cur_target.x - target_x) * (cur_target.x - target_x)) + ((cur_target.y - target_z) * (cur_target.y - target_z));
                    
                    // If this target has just fallen within the splash radius...
                    if ((cur_target_dist_squared <= cur_splash_radius_squared) && ((cur_target_dist_squared > prev_splash_radius_squared) || (prev_splash_radius_squared == 0)))
                    {
                        // Have any object in the attacked block become visible.
                        UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                        // Display the hit points decreasing, for damage being taken.
                        StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, 0f));

                        // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                        BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                    }
                }

                // If the splash radius has expanded to the full effect_radius, exit loop.
                if (cur_splash_radius >= (build_data.effect_radius + 0.5f)) {
                    break;
                }

                // Store the previous update's splash radius.
                prev_splash_radius_squared = cur_splash_radius_squared;

                // Wait a quarter second before the next update to the splash.
                yield return new WaitForSeconds(0.25f);
            }
        }
        else if (_build_type == BuildData.Type.AREA_EFFECT)
        {
            // Shuffle the list of targets into a random order.
            for (int i = 0; i < _targets.Count; i++) {
                 TargetRecord temp = _targets[i];
                 int randomIndex = UnityEngine.Random.Range(i, _targets.Count);
                 _targets[i] = _targets[randomIndex];
                 _targets[randomIndex] = temp;
             }

            TargetRecord cur_target;
            int num_targets = _targets.Count;
            float time_per_target = ((float)_duration) / num_targets;

            for (int target_index = 0; target_index < num_targets; target_index++)
	        {
                cur_target = _targets[target_index];

                // Have the object fire.
                if (block_data.build_object != null) 
                {
                    block_data.build_object.Fire(cur_target.x, cur_target.y, time_per_target);
                }

                // Have any object in the attacked block become visible.
                UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                // After delay of AREA_EFFECT_FIRE_DURATION, display the hit points decreasing, for damage being taken.
                StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, AREA_EFFECT_FIRE_DURATION));

                // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, AREA_EFFECT_FIRE_DURATION + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);

                yield return new WaitForSeconds(time_per_target);
            }
        }
        else if (_build_type == BuildData.Type.COUNTER_ATTACK)
        {
            TargetRecord cur_target = _targets[0];

            // Turn on the build object's FX emission.
            if (block_data.build_object != null) {
                block_data.build_object.SetEmission(true);
            }

            // Have any object in the attacked block become visible.
            UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

            // After delay of COUNTER_ATTACK_DAMAGE_START_TIME, display the hit points decreasing, for damage being taken.
            StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, COUNTER_ATTACK_DAMAGE_START_TIME));

            // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
            BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, COUNTER_ATTACK_DAMAGE_START_TIME + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);

            // If the block's build object is showing, change the object's aim to point toward the target block.
            if (block_data.build_object != null) {
                block_data.build_object.Aim(cur_target.x, cur_target.y, COUNTER_ATTACK_AIM_DURATION);
            }

            yield return new WaitForSeconds(COUNTER_ATTACK_AIM_DURATION);

            if (block_data.build_object != null) {
                block_data.build_object.DisplayRaid(_trigger_x, _trigger_z, cur_target.x, cur_target.y, COUNTER_ATTACK_RAID_DURATION);
            }

            yield return new WaitForSeconds(COUNTER_ATTACK_RAID_DURATION);

            // Turn off the build object's FX emission.
            if (block_data.build_object != null) {
                block_data.build_object.SetEmission(false);
            }
        }
        else if (_build_type == BuildData.Type.WIPE)
        {
            if (build_data.original_name.Contains("Dead Hand"))
            {
                if (build_data.original_name.Equals("Dead Hand"))
                {
                    // Trigger nuke particle effect
                    GameObject nuke = UnityEngine.Object.Instantiate(BuildPrefabs.instance.deadHand1Nuke) as GameObject;
                    nuke.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    Destroy(nuke, 20f); // Destroy the nuke effect after delay
                }
                else if (build_data.original_name.Equals("Dead Hand II"))
                {
                    // Trigger nuke particle effect
                    GameObject nuke = UnityEngine.Object.Instantiate(BuildPrefabs.instance.deadHand2Nuke) as GameObject;
                    nuke.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    Destroy(nuke, 20f); // Destroy the nuke effect after delay
                }
                else if (build_data.original_name.Equals("Dead Hand III"))
                {
                    // Trigger nuke particle effect
                    GameObject nuke = UnityEngine.Object.Instantiate(BuildPrefabs.instance.deadHand3Nuke) as GameObject;
                    nuke.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    Destroy(nuke, 20f); // Destroy the nuke effect after delay
                }

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.nuclear, MapView.instance.GetBlockCenterWorldPos(_x, _z), 1, 0);

                // Wait briefly before displaying damage
                yield return new WaitForSeconds(0.5f);

                // Iterate targets, and apply damage to those that have just fallen within the splash radius with this current update.
                for (int target_index = 0; target_index < _targets.Count; target_index++)
	            {
                    TargetRecord cur_target = _targets[target_index];

                    // Have any object in the attacked block become visible.
                    UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);
                
                    // Display the hit points decreasing, for damage being taken.
                    StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, 0f));

                    // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                    BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                }
            }
            else if (build_data.original_name.Contains("Geographic Wipe"))
            {
                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.geo_wipe, MapView.instance.GetBlockCenterWorldPos(_x, _z), 1, 0);
                Sound.instance.PlayInWorld(Sound.instance.nuclear, MapView.instance.GetBlockCenterWorldPos(_x, _z), 1, 0);

                // Iterate targets, and apply damage to those that have just fallen within the splash radius with this current update.
                for (int target_index = 0; target_index < _targets.Count; target_index++)
	            {
                    TargetRecord cur_target = _targets[target_index];

                    // Trigger flame particle effect
                    GameObject flame = UnityEngine.Object.Instantiate(BuildPrefabs.instance.geoWipeFlame) as GameObject;
                    flame.transform.position = MapView.instance.GetBlockCenterWorldPos(cur_target.x, cur_target.y);
                    Destroy(flame, 20f); // Destroy the flame effect after delay
    
                    // Have any object in the attacked block become visible.
                    UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);
                                
                    // Display the hit points decreasing, for damage being taken.
                    StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, 0f));

                    // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                    BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                }
            }
        }
        else if ((_build_type == BuildData.Type.GENERAL_LASTING_WIPE) || (_build_type == BuildData.Type.SPECIFIC_LASTING_WIPE))
        {
            if (build_data.original_name.Equals("Toxic Chemical Dump"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.toxChemDump1Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.toxic_chemical_dump, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Toxic Chemical Dump II"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.toxChemDump2Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.toxic_chemical_dump, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Toxic Chemical Dump III"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.toxChemDump3Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.toxic_chemical_dump, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Supervirus Contagion"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.supVirCont1Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.supervirus_contagion, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Supervirus Contagion II"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.supVirCont2Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.supervirus_contagion, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Supervirus Contagion III"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.supVirCont3Fog) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                Sound.instance.PlayInWorld(Sound.instance.supervirus_contagion, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Hypnotic Inducer"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.hypnoticInd1FX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + _duration);
                Sound.instance.PlayInWorld(Sound.instance.hypnotic_inducer, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Hypnotic Inducer II"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.hypnoticInd2FX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + _duration);
                Sound.instance.PlayInWorld(Sound.instance.hypnotic_inducer, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Equals("Hypnotic Inducer III"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.hypnoticInd3FX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + _duration);
                Sound.instance.PlayInWorld(Sound.instance.hypnotic_inducer, fx_object.transform.position);

                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }
            }
            else if (build_data.original_name.Contains("Temple"))
            {
                GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.temple1FX) as GameObject;
                fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                //fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + _duration);
                //UnityEngine.Object.Destroy(fx_object, _duration);
                AlienCreatureCharacter alienCharacter = fx_object.transform.GetChild(0).gameObject.GetComponent<AlienCreatureCharacter> ();
                alienCharacter.Attack(); // Attack Jump Hit Down StandUp
                Sound.instance.PlayInWorld(Sound.instance.zoth, fx_object.transform.position, 1, 0);
            }

            yield return new WaitForSeconds(1f);

            int num_targets = _targets.Count;
            TargetRecord cur_target;
            float wipe_duration = _duration - 3f;
            float wipe_start_time = Time.time;
            float cur_wipe_radius, cur_wipe_radius_squared, prev_wipe_radius_squared = 0, cur_target_dist_squared;

            // Loop to spread wipe over time.
            for (;;)
            {
                // Determine the radius of the wipe for this current update.
                cur_wipe_radius = (Time.time - wipe_start_time) * (float)build_data.effect_radius / wipe_duration + 0.5f;
                cur_wipe_radius_squared = cur_wipe_radius * cur_wipe_radius;

                // Iterate targets, and apply damage to those that have just fallen within the wipe radius with this current update.
                for (int target_index = 0; target_index < num_targets; target_index++)
	            {
                    cur_target = _targets[target_index];
                    cur_target_dist_squared = ((cur_target.x - _x) * (cur_target.x - _x)) + ((cur_target.y - _z) * (cur_target.y - _z));
                    
                    // If this target has just fallen within the splash radius...
                    if ((cur_target_dist_squared <= cur_wipe_radius_squared) && ((cur_target_dist_squared > prev_wipe_radius_squared) || (prev_wipe_radius_squared == 0)))
                    {
                        if (cur_target.GetWipeEndTime(start_action_time) != block_data.wipe_end_time)
                        {
                            // Get the target block's data.
                            BlockData target_block_data = GetBlockData(cur_target.x, cur_target.y);

                            if (target_block_data != null)
                            {
                                // Record the target block's new lasting wipe information.
                                target_block_data.wipe_end_time = cur_target.GetWipeEndTime(start_action_time);
                                target_block_data.wipe_nationID = cur_target.wipe_nationID;
                                target_block_data.wipe_flags = cur_target.wipe_flags;

                                // If appropriate, display a lasting wipe effect.
                                if ((target_block_data.wipe_end_time > Time.time) && (((target_block_data.wipe_flags & BuildData.WIPE_FLAG_GENERAL) != 0) || (((target_block_data.wipe_flags & BuildData.WIPE_FLAG_SPECIFIC) != 0) && ((GameData.instance.mapMode == GameData.MapMode.REPLAY) || (target_block_data.wipe_nationID == gameData.nationID))))) {
                                    DisplayLastingWipe(cur_target.x, cur_target.y, target_block_data, true);
                                }
                            }
                        }

                        // Have any object in the attacked block become visible.
                        UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                        // Display the hit points decreasing, for damage being taken.
                        StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, 0f));

                        // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                        BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                    }
                }

                // If the wipe radius has expanded to the full effect_radius, exit loop.
                if (cur_wipe_radius >= (build_data.effect_radius + 0.5f)) {
                    break;
                }

                // Store the previous update's wipe radius.
                prev_wipe_radius_squared = cur_wipe_radius_squared;

                // Wait a quarter second before the next update to the wipe.
                yield return new WaitForSeconds(0.25f);
            }
        }
        else if (_build_type == BuildData.Type.TOWER_BUSTER)
        {
            TargetRecord cur_target;
            int num_targets = _targets.Count;

            // Wait before showing tower buster attack, so that triggering attack will be displayed first.
            yield return new WaitForSeconds(TOWER_BUSTER_BEGIN_DELAY);
            
            for (int target_index = 0; target_index < num_targets; target_index++)
	        {
                cur_target = _targets[target_index];

                // Record the block's new target block.
                block_data.target_x = cur_target.x;
                block_data.target_z = cur_target.y;

                // Have the object fire.
                if (block_data.build_object != null) {
                    block_data.build_object.Fire(cur_target.x, cur_target.y, TOWER_BUSTER_FIRE_DURATION);
                }

                // Have any object in the attacked block become visible.
                UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

                // After delay of TOWER_BUSTER_FIRE_DURATION, display the hit points decreasing, for damage being taken.
                StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, TOWER_BUSTER_FIRE_DURATION));

                //Debug.Log("Tower buster at " + _x + "," + _z + " target " + target_index + " block " + cur_target.x + "," + cur_target.y + ": nationID: " + cur_target.newNationID + ", target start hp: " + cur_target.start_hit_points + ", target end hp: " + cur_target.end_hit_points);

                // If the next target is not this same block, then set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
                if ((target_index == (num_targets - 1)) || (cur_target.x != _targets[target_index + 1].x) || (cur_target.y != _targets[target_index + 1].y)) {
	                BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, TOWER_BUSTER_FIRE_DURATION + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);
                }

                yield return new WaitForSeconds(TOWER_BUSTER_FIRE_DURATION);
	        }
        }
        else if (_build_type == BuildData.Type.AREA_FORTIFICATION)
        {
            if (build_data.original_name.Contains("Roots of Despair"))
            {
                if (build_data.original_name.Equals("Roots of Despair"))
                {
                    GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.rootsDesp1FX) as GameObject;
                    fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + ROOTS_OF_DESPAIR_DURATION);
                }
                else if (build_data.original_name.Equals("Roots of Despair II"))
                {
                    GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.rootsDesp2FX) as GameObject;
                    fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + ROOTS_OF_DESPAIR_DURATION);
                }
                else if (build_data.original_name.Equals("Roots of Despair III"))
                {
                    GameObject fx_object = UnityEngine.Object.Instantiate(BuildPrefabs.instance.rootsDesp3FX) as GameObject;
                    fx_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);
                    fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + ROOTS_OF_DESPAIR_DURATION);
                }

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.gore, MapView.instance.GetBlockCenterWorldPos(_x, _z));
            }
            else if (build_data.original_name.Contains("Tree Summoner"))
            {
                // Have the object activate.
                if (block_data.build_object != null) {
                    block_data.build_object.Activate();
                }

                // Iterate targets, and display particle effect in each.
                for (int target_index = 0; target_index < _targets.Count; target_index++)
	            {
                    TargetRecord cur_target = _targets[target_index];

                    // Trigger particle effect
                    GameObject fx = UnityEngine.Object.Instantiate(BuildPrefabs.instance.treeSummonerFX) as GameObject;
                    fx.transform.position = MapView.instance.GetBlockCenterWorldPos(cur_target.x, cur_target.y);
                    Destroy(fx, 10f); // Destroy the flame effect after delay
                }

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.walkthroughleaves, MapView.instance.GetBlockCenterWorldPos(_x, _z));
            }
        }
        else if (_build_type == BuildData.Type.AIR_DROP)
        {
            TargetRecord cur_target = _targets[0];

            /// If the block's build object is showing, change the object's aim to point toward the target block, and turn on the FX emission.
            if (block_data.build_object != null) 
            {
                //block_data.build_object.Aim(cur_target.x, cur_target.y, 0f);
                //block_data.build_object.SetEmission(true);
                block_data.build_object.Fire(cur_target.x, cur_target.y, AIR_DROP_DAMAGE_START_TIME + DAMAGE_DURATION);
            }

            // Have any object in the attacked block become visible.
            UpdateObjectInvisibilityForAttack(cur_target.x, cur_target.y);

            // After delay of COUNTER_ATTACK_DAMAGE_START_TIME, display the hit points decreasing, for damage being taken.
            StartCoroutine(DelayedDisplayHitPoints(cur_target.x, cur_target.y, cur_target.start_hit_points, cur_target.end_hit_points, cur_target.full_hit_points, DAMAGE_DURATION, DisplayHitPoints.TransitionType.RANDOM, false, true, AIR_DROP_DAMAGE_START_TIME));

            // Set up a BlockProcess to show block transition (if appropriate) and regaining of hit points.
            BlockProcess.ActivateNew(cur_target.x, cur_target.y, BlockProcess.PROCESS_BATTLE, cur_target.newNationID, AIR_DROP_DAMAGE_START_TIME + DAMAGE_DURATION, cur_target.new_cur_hit_points, cur_target.new_full_hit_points, cur_target.new_full_hit_points, cur_target.hit_points_rate, cur_target.battle_flags);

            yield return new WaitForSeconds(AIR_DROP_DAMAGE_START_TIME + DAMAGE_DURATION);

            //// Turn off the build object's FX emission.
            //if (block_data.build_object != null) {
            //    block_data.build_object.SetEmission(false);
            //}
        }
    }

    public void InitTriggerInert(int _x, int _z)
    {
        Debug.Log("InitTriggerInert() called for " + _x + "," + _z);
        StartCoroutine(DisplayTriggerInert_Coroutine(_x, _z));
    }

    public IEnumerator DisplayTriggerInert_Coroutine(int _x, int _z)
    {
        yield return new WaitForSeconds(UnityEngine.Random.Range(0.1f, 0.6f));

        Vector3 effect_position = GetBlockCenterWorldPos(_x, _z) + new Vector3(0, 4, 0);

        // Play the sound associated with this effect.
        Sound.instance.PlayInWorld(Sound.instance.trigger_inert, effect_position);

        // Position and play the particle effect.
        GameObject triggerInertObject = MemManager.instance.GetTriggerInertParticleObject();
        triggerInertObject.transform.position = effect_position;
        triggerInertObject.transform.GetChild(0).GetComponent<ParticleSystem>().Play();

        yield return new WaitForSeconds(3f);

        // Stop and release the particle effect.
        triggerInertObject.transform.GetChild(0).GetComponent<ParticleSystem>().Stop();
        MemManager.instance.ReleaseTriggerInertParticleObject(triggerInertObject);
    }

    public IEnumerator DelayedDisplayHitPoints(int _x, int _z, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _duration, DisplayHitPoints.TransitionType _transition_type, bool _fade_out, bool _battle, float _delay)
    {
        yield return new WaitForSeconds(_delay);
        DisplayHitPoints.ActivateNew(_x, _z, _hit_points_start, _hit_points_end, _hit_points_full, _duration, _transition_type, _fade_out, _battle);
    }

    public IEnumerator DelayedDisplayTimer(int _x, int _z, DisplayTimer.Type _type, float _start_time, float _end_time, float _delay)
    {
        yield return new WaitForSeconds(_delay);

        // Get the block's data.
        BlockData block_data = GetBlockData(_x, _z);

        // If the block is still in view and is not set to crumble, display the timer showing time until object can next activate.
        if ((block_data != null) && block_data.in_view_area && ((block_data.crumble_time == -1) || (block_data.crumble_time < Time.time))) {
            DisplayTimer.ActivateNew(_x, _z, _type, _start_time, _end_time);
        }
    }

    public void UpdateObjectInvisibilityForAttack(int _block_x, int _block_z)
    {
        BlockData block_data = GetBlockData(_block_x, _block_z);

        // If this block contains an object that can be made invisible...
        if ((block_data != null) && (block_data.invisible_time != -1))
        {
            // Set the object to be visible for duration secondsRemainVisible.
            block_data.invisible_time = Time.time + GameData.instance.secondsRemainVisible;

            // If there is a BuildObject representin this block's object, update its invisibility.
            if (block_data.build_object != null) {
                block_data.build_object.UpdateInvisibility();
            }
        }
    }

    public bool ActivateContextMenu(int _blockX, int _blockZ, bool _press_and_hold=false)
    {
        return Woc.ContextMenu.instance.Activate(_blockX, _blockZ, _press_and_hold);
    }

    public void AdminClick(int _x, int _z)
    {
        BlockData block_data = GetBlockData(_x, _z);

        if (block_data == null) {
          return;
        }

        if ((block_data.objectID != -1) && (block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID))
        {
            BuildData build_data = BuildData.GetBuildData(block_data.objectID);

            // Determine a test target position
            int target_x = _x + UnityEngine.Random.Range(-2, 1);
            if (target_x >= _x) target_x++;
            int target_z = _z + UnityEngine.Random.Range(-2, 1);
            if (target_z >= _z) target_z++;
            BlockData target_block_data = GetBlockData(target_x, target_z);
            List <TargetRecord> targets = new List<TargetRecord>();
            TargetRecord cur_target = new TargetRecord();
            cur_target.x = target_x;
            cur_target.y = target_z;
            cur_target.newNationID = target_block_data.nationID;
            cur_target.full_hit_points = 10;
            cur_target.start_hit_points = 10;
            cur_target.end_hit_points = 5;
            cur_target.new_full_hit_points = 10;
            cur_target.hit_points_rate = 30;
            cur_target.battle_flags = (int)GameData.BattleFlags.TARGET_BLOCK;
            cur_target.wipe_end_time = -1f;
            cur_target.wipe_nationID = -1;
            cur_target.wipe_flags = 0;
            targets.Add(cur_target);

            int trigger_x = target_x;
            int trigger_z = target_z;
            if ((build_data.type == BuildData.Type.COUNTER_ATTACK) || (build_data.trigger_on == BuildData.TriggerOn.DIRECT_ATTACK))
            {
                trigger_x = _x;
                trigger_z = _z;
            }

            // Initialize test tower action.
            //Debug.Log("trigger_x: " + trigger_x + ", trigger_z: " + trigger_z + ", target_x: " + target_x + ", target_z: " + target_z);
            InitTowerAction(_x, _z, block_data.objectID, build_data.type, -1, 6, trigger_x, trigger_z, -1, targets);
        }
    }

    public void UpdateMapGUI()
    {
        // Update the view GUI
        DisplayAttack.UpdateAllScreenPosition();
        DisplayProcess.UpdateAllScreenPosition();
        DisplayHitPoints.UpdateAllScreenPosition();
        DisplayTimer.UpdateAllScreenPosition();
    }

    public void UpdateViewArea(bool _exit_all = false)
    {
        // Record the previous corners of the view area
        IntVector2 prev_view_left_block = view_left_block;
        IntVector2 prev_view_top_block = view_top_block;
        IntVector2 prev_view_right_block = view_right_block;
        IntVector2 prev_view_bottom_block = view_bottom_block;

        if (_exit_all)
        {
            view_left_block = new IntVector2(-1, -1);
            view_top_block = new IntVector2(-1, -1);
            view_right_block = new IntVector2(-1, -1);
            view_bottom_block = new IntVector2(-1, -1);
        }
        else
        {
            // Determine the new corners of the view area
            view_left_block = this.WorldPointToWorldBlock(this.GetWorldPointFromViewportPosition(new Vector3(0f,0f))) + VIEW_LEFT_BLOCK_MARGIN;
            view_top_block = this.WorldPointToWorldBlock(this.GetWorldPointFromViewportPosition(new Vector3(0f, 1f))) + VIEW_TOP_BLOCK_MARGIN;
            view_right_block = this.WorldPointToWorldBlock(this.GetWorldPointFromViewportPosition(new Vector3(1f, 1f))) + VIEW_RIGHT_BLOCK_MARGIN;
            view_bottom_block = this.WorldPointToWorldBlock(this.GetWorldPointFromViewportPosition(new Vector3(1f, 0f))) + VIEW_BOTTOM_BLOCK_MARGIN;
        }

        //Debug.Log("UpdateViewArea() view_top_block: " + view_top_block.x + "," + view_top_block.z + ", view_left_block: " + view_left_block.x + "," + view_left_block.z);
        //Debug.Log("UpdateViewArea() view_right_block: " + view_right_block.x + "," + view_right_block.z + ", view_bottom_block: " + view_bottom_block.x + "," + view_bottom_block.z);

        // Determine which blocks have exited and which blocks have entered the view area.
        if ((view_left_block.Equals(prev_view_left_block) == false) || (view_top_block.Equals(prev_view_top_block) == false) || (view_right_block.Equals(prev_view_right_block) == false) || (view_bottom_block.Equals(prev_view_bottom_block) == false))
        {
            //Debug.Log("View area changed. Prev left: " + prev_view_left_block.x + "," + prev_view_left_block.z + ", Prev top: " + prev_view_top_block.x + "," + prev_view_top_block.z + ", Prev right: " + prev_view_right_block.x + "," + prev_view_right_block.z + ", Prev bottom: " + prev_view_bottom_block.x + "," + prev_view_bottom_block.z + "; Left: " + view_left_block.x + "," + view_left_block.z + ", Top: " + view_top_block.x + "," + view_top_block.z + ", Right: " + view_right_block.x + "," + view_right_block.z + ", Bottom: " + view_bottom_block.x + "," + view_bottom_block.z);

            int x, z, z0, z1, new_start_x, new_end_x, prev_start_x, prev_end_x, local_chunk_z;

            // Constrain z to within the previous view area and within the local block data represented by chunks.
            z0 = Math.Max(prev_view_top_block.z, chunkDataBlockZ0);
            z1 = Math.Min(prev_view_bottom_block.z, chunkDataBlockZ1);

            // Iterate through each z row within the previous view area, to find all blocks that have just exited the view area.
            for (z = z0; z <= z1; z++)
            {
                // Determine z coord of local chunk that the current block is within.
                local_chunk_z = WorldBlockToLocalChunkZ(z);

                // If the current block's chunk is outside the local area, skip it.
                if ((local_chunk_z < 0) || (local_chunk_z >= VIEW_DATA_CHUNKS_WIDE)) {
                    continue;
                }

                // Determine the leftmost block x of this z row in the new view area. 
                if (z <= view_left_block.z) {
                    new_start_x = InterpXGivenZ(view_top_block, view_left_block, z);
                } else {
                    new_start_x = InterpXGivenZ(view_left_block, view_bottom_block, z);
                }

                // Determine the rightmost block x of this z row in the new view area.
                if (z <= view_right_block.z) {
                    new_end_x = InterpXGivenZ(view_top_block, view_right_block, z);
                } else {
                    new_end_x = InterpXGivenZ(view_right_block, view_bottom_block, z);
                }

                // Determine the leftmost block x of this z row in the previous view area.
                if (z <= prev_view_left_block.z) {
                    prev_start_x = InterpXGivenZ(prev_view_top_block, prev_view_left_block, z);
                } else {
                    prev_start_x = InterpXGivenZ(prev_view_left_block, prev_view_bottom_block, z);
                }

                // Determine the rightmost block x of this z row in the previous view area.
                if (z <= prev_view_right_block.z) {
                    prev_end_x = InterpXGivenZ(prev_view_top_block, prev_view_right_block, z);
                } else {
                    prev_end_x = InterpXGivenZ(prev_view_right_block, prev_view_bottom_block, z);
                }

                // Constrain x coords to be within the local block data represented by chunks.
                new_start_x = Math.Max(new_start_x, chunkDataBlockX0);
                new_end_x = Math.Min(new_end_x, chunkDataBlockX1);
                prev_start_x = Math.Max(prev_start_x, chunkDataBlockX0);
                prev_end_x = Math.Min(prev_end_x, chunkDataBlockX1);

                //Debug.Log("prev view z: " + z + ", prev_start_x: " + prev_start_x + ", prev_end_x: " + prev_end_x + ", new_start_x: " + new_start_x + ", new_end_x: " + new_end_x);

                if ((z >= view_top_block.z) && (z <= view_bottom_block.z) && (new_start_x <= new_end_x))
                {
                    // There is some new view area in the current z row...

                    // Each block from the start of the previous view area up to the start of the new view area has exited the view area.
                    for (x = prev_start_x; (x <= prev_end_x) && (x < new_start_x); x++)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockExitsViewArea(x, z);
                    }

                    // Each block from the end of the previous view area down to the end of the new view area has exited the view area.
                    for (x = prev_end_x; (x >= prev_start_x) && (x > new_end_x); x--)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        //Debug.Log("x:" + x + ", WorldBlockToLocalChunkX(x):" + WorldBlockToLocalChunkX(x) + ", local_chunk_z: " + local_chunk_z + ", new_start_x:" + new_start_x + ", new_end_x:" + new_end_x + ", prev_start_x:" + prev_start_x + ", prev_end_x:" + prev_end_x + ", chunkDataBlockX0:" + chunkDataBlockX0 + ", chunkDataBlockX1:" + chunkDataBlockX1);
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockExitsViewArea(x, z);
                    }
                }
                else
                {
                    // There is no new view area in the current row of the previous view area, so all blocks in this row have exited the view area.
                    for (x = prev_start_x; x <= prev_end_x; x++)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockExitsViewArea(x, z);
                    }
                }
            }

            // Constrain z to within the new view area and within the local block data.
            z0 = Math.Max(view_top_block.z, chunkDataBlockZ0);
            z1 = Math.Min(view_bottom_block.z, chunkDataBlockZ1);
            //Debug.Log("chunkDataBlockZ0: " + chunkDataBlockZ0+ ", chunkDataBlockZ1: " + chunkDataBlockZ1 + ", Z rows to iterate: " + z0 + " through " + z1); // TESTING

            // Iterate through each z row within the new view area, to find all blocks that have just entered the view area.
            for (z = z0; z <= z1; z++)
            {
                // Determine z coord of local chunk that the current block is within.
                local_chunk_z = WorldBlockToLocalChunkZ(z);
              
                // If the current block's chunk is outside the local area, skip it.
                if ((local_chunk_z < 0) || (local_chunk_z >= VIEW_DATA_CHUNKS_WIDE)) {
                    continue;
                }

                // Determine the leftmost block x of this z row in the new view area. 
                if (z <= view_left_block.z) {
                    new_start_x = InterpXGivenZ(view_top_block, view_left_block, z);
                } else {
                    new_start_x = InterpXGivenZ(view_left_block, view_bottom_block, z);
                }

                // Determine the rightmost block x of this z row in the new view area.
                if (z <= view_right_block.z) {
                    new_end_x = InterpXGivenZ(view_top_block, view_right_block, z);
                } else {
                    new_end_x = InterpXGivenZ(view_right_block, view_bottom_block, z);
                }

                // Determine the leftmost block x of this z row in the previous view area.
                if (z <= prev_view_left_block.z) {
                    prev_start_x = InterpXGivenZ(prev_view_top_block, prev_view_left_block, z);
                } else {
                    prev_start_x = InterpXGivenZ(prev_view_left_block, prev_view_bottom_block, z);
                }

                // Determine the rightmost block x of this z row in the previous view area.
                if (z <= prev_view_right_block.z) {
                    prev_end_x = InterpXGivenZ(prev_view_top_block, prev_view_right_block, z);
                } else {
                    prev_end_x = InterpXGivenZ(prev_view_right_block, prev_view_bottom_block, z);
                }

                // Constrain x coords to be within the local block data.
                new_start_x = Math.Max(new_start_x, chunkDataBlockX0);
                new_end_x = Math.Min(new_end_x, chunkDataBlockX1);
                prev_start_x = Math.Max(prev_start_x, chunkDataBlockX0);
                prev_end_x = Math.Min(prev_end_x, chunkDataBlockX1);

                //Debug.Log("new view z: " + z + ", prev_start_x: " + prev_start_x + ", prev_end_x: " + prev_end_x + ", new_start_x: " + new_start_x + ", new_end_x: " + new_end_x + ", chunkDataBlockX0: " + chunkDataBlockX0);

                if ((z >= prev_view_top_block.z) && (z <= prev_view_bottom_block.z) && (prev_start_x <= prev_end_x))
                {
                    // There is some prev view area in the current z row...

                    // Each block from the start of the new view area up to the start of the prev view area has entered the view area.
                    for (x = new_start_x; (x <= new_end_x) && (x < prev_start_x); x++)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        //Debug.Log("x:" + x + ", WorldBlockToLocalChunkX(x):" + WorldBlockToLocalChunkX(x) + ", local_chunk_z: " + local_chunk_z + ", new_start_x:" + new_start_x + ", new_end_x:" + new_end_x + ", prev_start_x:" + prev_start_x + ", prev_end_x:" + prev_end_x + ", chunkDataBlockX0:" + chunkDataBlockX0 + ", chunkDataBlockX1:" + chunkDataBlockX1);
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockEntersViewArea(x, z);
                    }

                    // Each block from the end of the new view area down to the end of the prev view area has entered the view area.
                    for (x = new_end_x; (x >= new_start_x) && (x > prev_end_x); x--)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        //Debug.Log("x:" + x + ", WorldBlockToLocalChunkX(x):" + WorldBlockToLocalChunkX(x) + ", local_chunk_z: " + local_chunk_z + ", new_start_x:" + new_start_x + ", new_end_x:" + new_end_x + ", prev_start_x:" + prev_start_x + ", prev_end_x:" + prev_end_x + ", chunkDataBlockX0:" + chunkDataBlockX0 + ", chunkDataBlockX1:" + chunkDataBlockX1);
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockEntersViewArea(x, z);
                    }
                }
                else
                {
                    // There is no previous view area in the current row of the new view area, so all blocks in this row have entered the view area.
                    for (x = new_start_x; x <= new_end_x; x++)
                    {
                        // If the current block's chunk is pending new data, skip it.
                        if (terrainPatches[WorldBlockToLocalChunkX(x), local_chunk_z].pendingData) {
                            continue;
                        }

                        BlockEntersViewArea(x, z);
                    }
                }
            }
        }

        // Now that the full view area has been updated, call UpdateViewAreaComplete().
        UpdateViewAreaComplete();
    }

    public void UpdateViewAreaForChunkDataReceived(int _local_chunk_x, int _local_chunk_z)
    {
        int x, z, start_x, end_x;

        //Debug.Log("UpdateViewAreaForChunkDataReceived(" + _local_chunk_x + "," + _local_chunk_z + ") Left: " + view_left_block.x + "," + view_left_block.z + ", Top: " + view_top_block.x + "," + view_top_block.z + ", Right: " + view_right_block.x + "," + view_right_block.z + ", Bottom: " + view_bottom_block.x + "," + view_bottom_block.z);

        // Determine bounds of the given local chunk, in global block coordinates.
        int global_block_x0 = (viewChunkX0 + _local_chunk_x) * CHUNK_SIZE;
        int global_block_x1 = (viewChunkX0 + _local_chunk_x + 1) * CHUNK_SIZE - 1;
        int global_block_z0 = (viewChunkZ0 + _local_chunk_z) * CHUNK_SIZE;
        int global_block_z1 = (viewChunkZ0 + _local_chunk_z + 1) * CHUNK_SIZE - 1;

        // Determine the range of z rows to iterate -- the overlap between the given chunk and the view area.
        int z0 = Math.Max(view_top_block.z, global_block_z0);
        int z1 = Math.Min(view_bottom_block.z, global_block_z1);

        //Debug.Log("global_block_x0: " + global_block_x0 + ", global_block_x1: " + global_block_x1 + ", global_block_z0: " + global_block_z0 + ", global_block_z1: " + global_block_z1 + ", z0: " + z0 + ", z1: " + z1);

        // Iterate through each z row within the new view area and within the given chunk, to find all blocks that have entered the view area.
        for (z = z0; z <= z1; z++) 
        {
            // Determine the leftmost block x of this z row in the view area. 
            if (z <= view_left_block.z) {
                start_x = InterpXGivenZ(view_top_block, view_left_block, z);
            } else {
                start_x = InterpXGivenZ(view_left_block, view_bottom_block, z);
            }

            // Determine the rightmost block x of this z row in the view area.
            if (z <= view_right_block.z) {
                end_x = InterpXGivenZ(view_top_block, view_right_block, z);
            } else {
                end_x = InterpXGivenZ(view_right_block, view_bottom_block, z);
            }

            // Bound the start and end x coords to be contained by the given chunk.
            start_x = Math.Max(start_x, global_block_x0);
            end_x = Math.Min(end_x, global_block_x1);

            // Call BlockEntersViewArea() on each block that is both within the view area and within the given chunk.
            for (x = start_x; x <= end_x; x++) {
                BlockEntersViewArea(x, z);
            }
        }
    }

    public int InterpXGivenZ(IntVector2 _line_high_point, IntVector2 _line_low_point, int _z)
    {
        return (_line_low_point.z == _line_high_point.z) ? _line_low_point.x : (_line_high_point.x + ((_line_low_point.x - _line_high_point.x) * (_z - _line_high_point.z) / (_line_low_point.z - _line_high_point.z)));
    }

    public void BlockExitsViewArea(int _x, int _z)
    {
        //Debug.Log("Block exits view area: " + _x + "," + _z);

        // Get the data for the given block.
        BlockData block_data = blocks[_x - viewDataBlockX0, _z - viewDataBlockZ0];

        // TESTING
        if (block_data.in_view_area == false) {
            Debug.Log("BlockExitsViewArea() called for block " + _x + "," + _z + " that is already not in view area!");
            //GameGUI.instance.LogToChat("BlockExitsViewArea() error");
            return;
        }

        // Record that it is not in the view area.
        block_data.in_view_area = false;

        if (block_data.build_object != null)
        {
            // Remove the BuildObject that represented this block's 3D object.
            block_data.build_object.CleanUp();
            block_data.build_object = null;
        }

        if (block_data.landscape_object != null)
        {
            // Remove the LandscapeObject that represented this block's 3D object.
            block_data.landscape_object.CleanUp();
            block_data.landscape_object = null;
        }

        // Remove any limit boundry lines.
        RemoveBoundaryLines(block_data);

        if (block_data.wipe_object != null)
        {
            // Destroy the object representing this block's lasting wipe.
            UnityEngine.Object.Destroy(block_data.wipe_object);
            block_data.wipe_object = null;
        }

        // If the block has a surround count, remove it.
        if (block_data.surround_count != null) {
            RemoveSurroundCount(block_data);
        }
 
        // If this block had a nation label, remove it.
        if (block_data.label_nationID != -1) {
            //Debug.Log("BlockExitsViewArea() removing label from block " + _x + "," + _z);
            RemoveLabelFromBlock(block_data);
        }

        // Decrement the patch's count of the the number of its blocks in view.
        PatchData patch_data = terrainPatches[WorldBlockToLocalChunkX(_x), WorldBlockToLocalChunkZ(_z)];
        patch_data.DecrementNumBlocksInView();
    }

    public void BlockEntersViewArea(int _x, int _z)
    {
        // Get the data for the given block.
        BlockData block_data = blocks[_x - viewDataBlockX0, _z - viewDataBlockZ0];

        //Debug.Log("Block enters view area: " + _x + "," + _z + ", objectID: " + block_data.objectID);

        // TESTING
        if (block_data.in_view_area == true) {
            //Debug.Log("BlockEntersViewArea() called for block " + _x + "," + _z + " that is already in view area!");
            return;
        }

        // Record that it is now in the view area.
        block_data.in_view_area = true;

        // If this block has a lasting wipe in effect, display it.
        if ((block_data.wipe_end_time > Time.time) && (((block_data.wipe_flags & BuildData.WIPE_FLAG_GENERAL) != 0) || (((block_data.wipe_flags & BuildData.WIPE_FLAG_SPECIFIC) != 0) && (block_data.wipe_nationID == gameData.nationID)))) {
            DisplayLastingWipe(_x, _z, block_data, false);
        }

        if (block_data.objectID != -1)
        {
            if (block_data.objectID >= ObjectData.RESOURCE_OBJECT_BASE_ID)
            {
                // Create a LandscapeObject to represent this block's 3D object.
                block_data.landscape_object = ((GameObject) Instantiate(landscapeObjectPrefab, Vector3.zero, Quaternion.identity)).GetComponent<LandscapeObject>();
                block_data.landscape_object.Initialize(_x, _z);
            }
            else
            {
                // Create a BuildObject to represent this block's 3D object.
                block_data.build_object = ((GameObject) Instantiate(buildObjectPrefab, Vector3.zero, Quaternion.identity)).GetComponent<BuildObject>();
                block_data.build_object.Initialize(_x, _z);

                // Set the block to display as inert, if appropriate.
                if (block_data.build_object.build_data.MayBecomeInert() && BlockIsToBeInert(_x, _z, block_data)) {
                    block_data.build_object.SetInert(true, false);
                }
            }
        }

        if (mapID == GameData.MAINLAND_MAP_ID)
        {
            // Display map position limit boundaries.
            CreateBoundaryLines(_x, _z, block_data);

            // Display nation extent limit boundaries.
            CreateMaxExtentLines(_x, _z, block_data);
        }

        // If this block belongs to the player's nation, set up the surround count if necessary.
        if (block_data.nationID == GameData.instance.nationID) {
            SetUpSurroundCount(_x, _z, block_data);
        }

        // Determine whether a nation label should be placed in this block.
        if (block_data.nationID != -1) {
            EvaluateBlockForNationLabel(block_data, _x, _z);
        }

        // Increment the patch's count of the the number of its blocks in view.
        PatchData patch_data = terrainPatches[WorldBlockToLocalChunkX(_x), WorldBlockToLocalChunkZ(_z)];
        patch_data.IncrementNumBlocksInView();
        //Debug.Log("Incremented vblock count for patch " + (localBlockX >> CHUNK_SIZE_SHIFT) + "," + (localBlockZ >> CHUNK_SIZE_SHIFT) + ": " + patch_data + ". Terrain active: " + patch_data.terrain.gameObject.activeInHierarchy);
    }

    public void UpdateViewAreaComplete()
    {
        // Create any nation labels that it was determined should be created.
        CreateNationLabels();
    }

    public void RemoveLabelFromBlock(BlockData _block_data)
    {
        NationData nation_data = gameData.nationTable[_block_data.label_nationID];

        if ((nation_data != null) && (nation_data.label != null))
        {
            //Debug.Log("Removing label for nation " + _block_data.label_nationID + " from block " + nation_data.label_block_x + "," + nation_data.label_block_z);

            // Remove and release the nation label.
            nation_data.label.GetComponent<NationLabel>().nationID = -1;
            nation_data.label.SetActive(false);
            MemManager.instance.ReleaseNationLabelObject(nation_data.label);
            active_nation_labels.Remove(nation_data.label.GetComponent<NationLabel>());

            // Remove the nation data's record of the label.
            nation_data.label = null;
            nation_data.label_block_x = -1;
            nation_data.label_block_z = -1;
            nation_data.label_score = -100;
        }

        // Remove the block's record of the label.
        _block_data.label_nationID = -1;
    }

    public void EvaluateBlockForNationLabel(BlockData _block_data, int _x, int _z)
    {
        if ((_block_data.nationID != -1)/* && (gameData.nationTable.ContainsKey(block_data.nationID))*/)
        {
            NationData nation_data = null;

            try 
            {
                nation_data = gameData.nationTable[_block_data.nationID];
            }
            catch (KeyNotFoundException e) 
            {
                Debug.Log("EvaluateBlockForNationLabel() for block " + _x + "," + _z + " nationID " + _block_data.nationID + " key not found in nationTable with count " + gameData.nationTable.Count);
                return;
            }
            
            // Don't label an incognito nation, unless it's the player's own nation.
            if ((nation_data != null) && (nation_data.label == null) && (((nation_data.flags & (int)GameData.NationFlags.INCOGNITO) == 0) || (_block_data.nationID == GameData.instance.nationID)))
            {
                int score = DetermineLabelLocationScore(_block_data.nationID, _x, _z);

                // Place a label for this nation at this block if this block's score is higher than the previous score for this nation,
                // and if either a label is required for this nation, or if the block's score is very high, or if there are few visible labels already.
                if ((score > nation_data.label_score) && (nation_data.label_required || (score > 6) || ((score >= 0) && ((active_nation_labels.Count + nations_to_label.Count) < 8))))
                {
                    nation_data.label_block_x = _x;
                    nation_data.label_block_z = _z;
                    nation_data.label_score = score;
                
                    if (nations_to_label.ContainsKey(_block_data.nationID) == false) {
                        nations_to_label.Add(_block_data.nationID, nation_data);
                    }
                }
            }
        }
    }

    public void CreateNationLabels()
    {
        // Create a label for each nation in the nations_to_label dictionary.
        foreach(KeyValuePair<int, NationData> entry in nations_to_label)
        {
            //Debug.Log("Placing label for nation " + entry.Key + " at block " + entry.Value.label_block_x + "," + entry.Value.label_block_z);

            // Set up a new NationLabelPrefab object to represent this label.
            GameObject nationLabelObject = MemManager.instance.GetNationLabelObject();
            nationLabelObject.transform.position = MapView.instance.GetBlockCenterWorldPos(entry.Value.label_block_x, entry.Value.label_block_z) + new Vector3(0, 0, -1);
            nationLabelObject.transform.localEulerAngles = new Vector3(90f, 45f, 0f);
            nationLabelObject.SetActive(true);
            NationLabel nation_label = nationLabelObject.GetComponent<NationLabel>();
            nation_label.SetText(entry.Value.GetName(entry.Key != GameData.instance.nationID));
            nation_label.SetAlpha(nation_label_alpha);
            active_nation_labels.Add(nation_label);
            nation_label.nationID = entry.Key;

            //Debug.Log("CreateNationLabels() added label for block " + entry.Value.label_block_x + "," + entry.Value.label_block_z);

            // Record the label in the NationData entry for this nation.
            entry.Value.label = nationLabelObject;

            // Record in the block, the ID of the nation whose label is being placed there.
            BlockData block_data = GetBlockData(entry.Value.label_block_x, entry.Value.label_block_z);
            block_data.label_nationID = entry.Key;

            //Debug.Log("Placed label at position " + ((entry.Value.label_block_x - chunkDataBlockX0 + 0.5f) * BLOCK_SIZE) + "," + (-((entry.Value.label_block_z - chunkDataBlockZ0 + 0.5f) * BLOCK_SIZE)));
        }

        // Clear the dictionary of nations to be labelled.
        nations_to_label.Clear();
    }

    public int DetermineLabelLocationScore(int _nationID, int _x, int _z)
    {
        int score = 0;
        int local_x = _x - viewDataBlockX0;
        int local_z = _z - viewDataBlockZ0;

        bool not_on_left_edge = (local_x > 0);
        bool not_on_right_edge = (local_x < (VIEW_DATA_BLOCKS_WIDE - 1));
        bool not_on_top_edge = (local_z > 0);
        bool not_on_bottom_edge = (local_z < (VIEW_DATA_BLOCKS_WIDE - 1));

        // Award points for surrounding blocks being part of the same nation, especially blocks displayed adjacent to left and right.
        // Subtract points for blocks displayed adjacent to left or right that are higher than flat land terrain.

        if (not_on_left_edge)
        {
            if (not_on_top_edge && (blocks[local_x - 1, local_z - 1].nationID == _nationID)) score += 2;
            if ((blocks[local_x - 1, local_z].nationID == _nationID)) score++;
            if (not_on_bottom_edge && (blocks[local_x - 1, local_z + 1].nationID == _nationID)) score++;

            if (not_on_top_edge && (GetBlockTerrain(_x - 1, _z - 1) > TERRAIN_FLAT_LAND)) score -= 3;
        }

        if (not_on_right_edge)
        {
            if (not_on_top_edge && (blocks[local_x + 1, local_z - 1].nationID == _nationID)) score++;
            if ((blocks[local_x + 1, local_z].nationID == _nationID)) score++;
            if (not_on_bottom_edge && (blocks[local_x + 1, local_z + 1].nationID == _nationID)) score += 2;

            if (not_on_bottom_edge && (GetBlockTerrain(_x + 1, _z + 1) > TERRAIN_FLAT_LAND)) score -= 3;
        }

        if (not_on_top_edge && (blocks[local_x, local_z - 1].nationID == _nationID)) score++;
        if (not_on_bottom_edge && (blocks[local_x, local_z + 1].nationID == _nationID)) score++;

        return score;
    }

    public void SetUpSurroundCount(int _blockX, int _blockZ, BlockData _block_data)
    {
        // If the block already has a surround count, remove it.
        if (_block_data.surround_count != null) {
            RemoveSurroundCount(_block_data);
        }

        // If the given block is not owned by the player's nation, do not display a surround count on it.
        if (_block_data.nationID != GameData.instance.nationID) {
            return;
        }

        // If there is not at least one block between the given block and the edge of the map data, do not display a surround count on this block.
        if ((_blockX <= viewDataBlockX0) || (_blockX >= viewDataBlockX1) || (_blockZ <= viewDataBlockZ0) || (_blockZ >= viewDataBlockZ1)) {
            return;
        }

        // Count how many enemy towers are adjacent to the given block.
        int count = 0;
        BlockData adj_block_data;
        for (int z = _blockZ - 1; z <= _blockZ + 1; z++)
        {
            for (int x = _blockX - 1; x <= _blockX + 1; x++)
            {
                // If the current adjacent block has an object that is owned by a nation other than the player's nation, 
                // and that block is occupied by the owner of that block's object (so that the object may be functional),
                // and if that object is not a wall, increase the count of surrounding towers.
                if ((x != _blockX) || (z != _blockZ))
                {
                    adj_block_data = GetBlockData(x, z);
                    if ((adj_block_data.objectID != -1) && (adj_block_data.owner_nationID != -1) && (adj_block_data.owner_nationID == adj_block_data.nationID) && (adj_block_data.owner_nationID != GameData.instance.nationID) && (BuildData.GetBuildData(adj_block_data.objectID).type != BuildData.Type.WALL)) {
                        count++;
                    }
                }
            }
        }

        // If there are no adjacent enemy towers, do not display a surround count.
        if (count == 0) {
            return;
        }

        String text_string;
        switch (count)
        {
            case 1: text_string = "I"; break;
            case 2: text_string = "II"; break;
            case 3: text_string = "III"; break;
            case 4: text_string = "IV"; break;
            case 5: text_string = "V"; break;
            case 6: text_string = "VI"; break;
            case 7: text_string = "VII"; break;
            default: text_string = "VIII"; break;
        }

        // Set up a new SurroundCountPrefab object to represent this block's surround count.
        GameObject surroundCountObject = MemManager.instance.GetSurroundCountObject();
        surroundCountObject.transform.position = MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ) + new Vector3(0, 0, 0);
        surroundCountObject.transform.localEulerAngles = new Vector3(90f, 45f, 0f);
        surroundCountObject.SetActive(true);
        SurroundCount surround_count = surroundCountObject.GetComponent<SurroundCount>();
        surround_count.SetText(text_string);
        surround_count.SetAlpha(surround_count_alpha);
        active_surround_counts.Add(surround_count);

        // Record this block's surround count object.
        _block_data.surround_count = surroundCountObject;
    }

    public void RemoveSurroundCount(BlockData _block_data)
    {
        if (_block_data.surround_count != null)
        {
            _block_data.surround_count.SetActive(false);
            MemManager.instance.ReleaseSurroundCountObject(_block_data.surround_count);
            active_surround_counts.Remove(_block_data.surround_count.GetComponent<SurroundCount>());
            _block_data.surround_count = null;
        }
    }

    public void UpdateNeighboringSurroundCounts(int _blockX, int _blockZ, bool _update_center_block)
    {
        // Set up the surround count of each block neighboring the given block.
        BlockData adj_block_data;
        for (int z = _blockZ - 1; z <= _blockZ + 1; z++)
        {
            for (int x = _blockX - 1; x <= _blockX + 1; x++)
            {
                if (_update_center_block || (x != _blockX) || (z != _blockZ))
                {
                    adj_block_data = GetBlockData(x, z);

                    if (adj_block_data != null) {
                        SetUpSurroundCount(x, z, adj_block_data);
                    }
                }
            }
        }
    }

    public void UpdateNationMaxExtent()
    {
        int newMainlandMaxExtentX0 = mainlandExtentX1 - nationMaxExtent;
        int newMainlandMaxExtentX1 = mainlandExtentX0 + nationMaxExtent;
        int newMainlandMaxExtentZ0 = mainlandExtentZ1 - nationMaxExtent;
        int newMainlandMaxExtentZ1 = mainlandExtentZ0 + nationMaxExtent;

        if ((newMainlandMaxExtentX0 != mainlandMaxExtentX0) ||
            (newMainlandMaxExtentX1 != mainlandMaxExtentX1) ||
            (newMainlandMaxExtentZ0 != mainlandMaxExtentZ0) ||
            (newMainlandMaxExtentZ1 != mainlandMaxExtentZ1))
        {
            // Update any visible mex extent lines.
            UpdateMaxExtentLines();

            // Record the new max extent values.
            mainlandMaxExtentX0 = newMainlandMaxExtentX0;
            mainlandMaxExtentX1 = newMainlandMaxExtentX1;
            mainlandMaxExtentZ0 = newMainlandMaxExtentZ0;
            mainlandMaxExtentZ1 = newMainlandMaxExtentZ1;
        }
    }

    public void UpdateMaxExtentLines()
    {
        BlockData block_data;

        // If the map view isn't currently valid, do nothing.
        if (viewBlockX == Int32.MinValue) {
            return;
        }

        // Iterate through all blocks..
        for (int z = 0; z < VIEW_DATA_BLOCKS_WIDE; z++)
        {
            for (int x = 0; x < VIEW_DATA_BLOCKS_WIDE; x++)
            {
                // Get the current block's data.
                block_data = GetBlockData(x + viewDataBlockX0, z + viewDataBlockZ0);

                // Skip blocks that are not in the current view.
                if (block_data.in_view_area == false) {
                    continue;
                }

                // Remove any max extent boundary lines.
                RemoveMaxExtentLines(block_data);
                
                if (mapID == GameData.MAINLAND_MAP_ID) {
                    CreateMaxExtentLines(x + viewDataBlockX0, z + viewDataBlockZ0, block_data);
                }
            }
        }
    }

    public void UpdateBoundaryLines()
    {
        BlockData block_data;

        // If the map view isn't currently valid, do nothing.
        if (viewBlockX == Int32.MinValue) {
            return;
        }

        // Iterate through all blocks..
        for (int z = 0; z < VIEW_DATA_BLOCKS_WIDE; z++)
        {
            for (int x = 0; x < VIEW_DATA_BLOCKS_WIDE; x++)
            {
                // Get the current block's data.
                block_data = GetBlockData(x + viewDataBlockX0, z + viewDataBlockZ0);

                // Skip blocks that are not in the current view.
                if (block_data.in_view_area == false) {
                    continue;
                }

                // Remove any limit boundary lines.
                RemoveBoundaryLines(block_data);
                
                if (mapID == GameData.MAINLAND_MAP_ID) {
                    CreateBoundaryLines(x + viewDataBlockX0, z + viewDataBlockZ0, block_data);
                }
            }
        }
    }

    public void CreateMaxExtentLines(int _x, int _z, BlockData _block_data)
    {
        // Create a max extent boundary object for this block if appropriate.
        if (((_x == mainlandMaxExtentX0) || (_x == mainlandMaxExtentX1)) && (_z >= mainlandMaxExtentZ0) && (_z <= mainlandMaxExtentZ1)) {
            _block_data.extent_limit_object_type = GameData.LimitType.LimitExtent;
        }
        else if (((_z == mainlandMaxExtentZ0) || (_z == mainlandMaxExtentZ1)) && (_x >= mainlandMaxExtentX0) && (_x <= mainlandMaxExtentX1)) {
            _block_data.extent_limit_object_type = GameData.LimitType.LimitExtent;
        }

        if (_block_data.extent_limit_object_type != GameData.LimitType.Undef) 
        {
            _block_data.extent_limit_object = MemManager.instance.GetLimitObject(_block_data.extent_limit_object_type);
            _block_data.extent_limit_object.transform.localPosition = GetBlockCenterWorldPosFlat(_x, _z);
        }
    }

    public void RemoveMaxExtentLines(BlockData _block_data)
    {
        if (_block_data.extent_limit_object != null)
        {
            // Remove the extent limit barrier object.
            MemManager.instance.ReleaseLimitObject(_block_data.extent_limit_object_type, _block_data.extent_limit_object);
            _block_data.extent_limit_object = null;
            _block_data.extent_limit_object_type = GameData.LimitType.Undef;
        }
    }

    public void CreateBoundaryLines(int _x, int _z, BlockData _block_data)
    {
        // Create a limit boundary object for this block if appropriate.
        if (_x == GameData.instance.map_position_limit) {
            _block_data.limit_object_type = GameData.LimitType.LimitWestern;
        }
        else if (_x == GameData.instance.map_position_limit_next_level) {
            _block_data.limit_object_type = GameData.LimitType.LimitWesternNextLevel;
        }
        else if (_x == GameData.instance.map_position_eastern_limit) {
            _block_data.limit_object_type = GameData.LimitType.LimitEastern;
        }

        if (_block_data.limit_object_type != GameData.LimitType.Undef) 
        {
            _block_data.limit_object = MemManager.instance.GetLimitObject(_block_data.limit_object_type);
            _block_data.limit_object.transform.localPosition = GetBlockCenterWorldPosFlat(_x, _z);
        }

        // Create a veteran area limit boundary object for this block if appropriate.
        if (_z == GameData.instance.newPlayerAreaBoundary) 
        {
            if (GameData.instance.nationIsVeteran || GameData.instance.userIsVeteran) {
                _block_data.vet_limit_object_type = GameData.LimitType.LimitVetArea;
            } else {
                _block_data.vet_limit_object_type = GameData.LimitType.LimitNewArea;
            }
        }

        if (_block_data.vet_limit_object_type != GameData.LimitType.Undef) 
        {
            _block_data.vet_limit_object = MemManager.instance.GetLimitObject(_block_data.vet_limit_object_type);
            _block_data.vet_limit_object.transform.localPosition = GetBlockCenterWorldPosFlat(_x, _z);
        }
    }

    public void RemoveBoundaryLines(BlockData _block_data)
    {
        if (_block_data.limit_object != null)
        {
            // Remove the limit barrier object.
            MemManager.instance.ReleaseLimitObject(_block_data.limit_object_type, _block_data.limit_object);
            _block_data.limit_object = null;
            _block_data.limit_object_type = GameData.LimitType.Undef;
        }

        if (_block_data.vet_limit_object != null)
        {
            // Remove the veteran limit barrier object.
            MemManager.instance.ReleaseLimitObject(_block_data.vet_limit_object_type, _block_data.vet_limit_object);
            _block_data.vet_limit_object = null;
            _block_data.vet_limit_object_type = GameData.LimitType.Undef;
        }
    }

    public void ModifiedExtendedData(int _blockX, int _blockZ)
    {
        BlockData block_data = GetBlockData(_blockX, _blockZ);

        if (block_data.in_view_area)
        {
            if (block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID)
            {
                if (block_data.build_object != null)
                {
                    // Fade and clean up the representation of the old version of this object, before creating the new version.
                    block_data.build_object.FadeAndCleanUp();
                }

                if (block_data.objectID != -1)
                {
                    // Create a BuildObject to represent this block's 3D object.
                    block_data.build_object = ((GameObject) Instantiate(buildObjectPrefab, Vector3.zero, Quaternion.identity)).GetComponent<BuildObject>();
                    block_data.build_object.Initialize(_blockX, _blockZ);

                    // Set the block to display as inert, if appropriate.
                    if (block_data.build_object.build_data.MayBecomeInert() && BlockIsToBeInert(_blockX, _blockZ, block_data)) {
                        Debug.Log("Setting new build inert");
                        block_data.build_object.SetInert(true, true);
                    }
                }
            }
            /*
            else if ((block_data.objectID == -1) && (block_data.build_object != null))
            {
                // Remove the BuildObject that represented this block's 3D object.
                block_data.build_object.CleanUp();
                block_data.build_object = null;
            }
            */

            // Update the block's neighbors' surround counts.
            UpdateNeighboringSurroundCounts(_blockX, _blockZ, false);
        }
    }

    public void SalvageBuildObject(int _blockX, int _blockZ)
    {
        if (!MapView.instance.BlockOutsideViewData(_blockX, _blockZ))
		{
           BlockData block_data = GetBlockData(_blockX, _blockZ);

           if ((block_data.in_view_area) && (block_data.build_object != null))
           {
                block_data.build_object.Salvage();
                block_data.build_object = null;
                block_data.objectID = -1;

                // Update the block's neighbors' surround counts.
                UpdateNeighboringSurroundCounts(_blockX, _blockZ, false);
           }
        }
    }

    public void CompleteBuildObject(int _blockX, int _blockZ)
    {
        if (!MapView.instance.BlockOutsideViewData(_blockX,_blockZ))
		{
           BlockData block_data = GetBlockData(_blockX, _blockZ);
           block_data.completion_time = Time.time;

           if ((block_data.in_view_area) && (block_data.build_object != null)) {
                block_data.build_object.Complete();
           }
        }
    }

    public void UpdateForInert()
    {
        int x, z, start_x, end_x;
        BlockData block_data;
        bool inert;

        // Iterate through each z row within the view area...
        for (z = view_top_block.z; z <= view_bottom_block.z; z++) 
        {
            // Determine the leftmost block x of this z row in the view area. 
            if (z <= view_left_block.z) {
                start_x = InterpXGivenZ(view_top_block, view_left_block, z);
            } else {
                start_x = InterpXGivenZ(view_left_block, view_bottom_block, z);
            }

            // Determine the rightmost block x of this z row in the view area.
            if (z <= view_right_block.z) {
                end_x = InterpXGivenZ(view_top_block, view_right_block, z);
            } else {
                end_x = InterpXGivenZ(view_right_block, view_bottom_block, z);
            }

            // Iterate through each block in the current row of the view area...
            for (x = start_x; x <= end_x; x++) 
            {
                block_data = GetBlockData(x, z);

                if ((block_data != null) && (block_data.build_object != null))
                {
                    // Determine whether the object in this block should be inert.
                    inert = block_data.build_object.build_data.MayBecomeInert() && BlockIsToBeInert(x, z, block_data);

                    // If the object's inert state needs to be changed, change it.
                    //Debug.Log("Block " + x + "," + z + " IsInert(): " + block_data.build_object.IsInert() + ", is to be inert: " + inert);
                    if (block_data.build_object.IsInert() != inert) {
                        block_data.build_object.SetInert(inert, true);
                    }
                }
            }
        }
    }

    public bool BlockIsToBeInert(int _blockX, int _blockZ, BlockData _block_data)
    {
        float geo_eff = GameData.instance.GetFinalGeoEfficiency();

        if ((geo_eff == 1f) && ((GameData.instance.current_footprint.energy_burn_rate == 0) || ((gameData.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy > 0)))) {
            return false;
        }

        if ((_block_data != null) && (_block_data.objectID != -1) && (_block_data.owner_nationID == GameData.instance.nationID) && (_block_data.nationID == GameData.instance.nationID) && (_block_data.build_object != null))
        {
            float inert_threshold = Mathf.Min(geo_eff, ((GameData.instance.mapMode == GameData.MapMode.MAINLAND) && (GameData.instance.energy > 0)) ? 1f : (float)GameData.instance.GetFinalEnergyRate() / (float)GameData.instance.GetFinalEnergyBurnRate());

            //Debug.Log("BlockIsToBeInert(" + _blockX + "," + _blockZ + ") energyRate: " + GameData.instance.energyRate + ", energyBurnRate: " + GameData.instance.current_footprint.energy_burn_rate + ", inert_threshold: " + inert_threshold + ", hash: " + GameData.instance.xxhash.GetHashFloat(_blockX, _blockZ) + " inert: " + ((GameData.instance.xxhash.GetHashFloat(_blockX, _blockZ) > inert_threshold) ? "YES" : "NO"));
            // Determine whether the object in this block should be inert.
            return (GameData.instance.xxhash.GetHashFloat(_blockX, _blockZ) > inert_threshold);
        }

        return false;
    }

    public void DisplayLastingWipe(int _x, int _z, BlockData _block_data, bool _starting)
    {
        // If this block already has a lasting wipe effect, cause it to end now.
        if (_block_data.wipe_object != null) {
            _block_data.wipe_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time);
        }

        // Determine which prefab to instantiate.
        GameObject wipe_effect_prefab = null;
        if ((_block_data.wipe_flags & BuildData.WIPE_FLAG_CHEMICAL) != 0)   wipe_effect_prefab = BuildPrefabs.instance.wipeChemical;
        if ((_block_data.wipe_flags & BuildData.WIPE_FLAG_SUPERVIRUS) != 0) wipe_effect_prefab = BuildPrefabs.instance.wipeSupervirus;
        if ((_block_data.wipe_flags & BuildData.WIPE_FLAG_HYPONOTIC) != 0)  wipe_effect_prefab = BuildPrefabs.instance.wipeHypnotic;
        if ((_block_data.wipe_flags & BuildData.WIPE_FLAG_TEMPLE) != 0)     wipe_effect_prefab = BuildPrefabs.instance.wipeTemple;
        //Debug.Log("Flags: " + _block_data.wipe_flags + ", wipe_effect_prefab: " + wipe_effect_prefab);

        // Instantiate and position the object.
        _block_data.wipe_object = UnityEngine.Object.Instantiate(wipe_effect_prefab) as GameObject;
        _block_data.wipe_object.transform.position = MapView.instance.GetBlockCenterWorldPos(_x, _z);

        if (!_starting) 
        {
            // Fast forward past the beginning of the particle effect.
            _block_data.wipe_object.GetComponent<ParticleFXManager>().PreWarm();
        }

        // Set the object to disappear when the lasting wipe ends.
        _block_data.wipe_object.GetComponent<ParticleFXManager>().SetEndTime(_block_data.wipe_end_time);
    }

    public void SetCameraZoomDistance(float _camera_zoom_distance)
    {
        //Debug.Log("SetCameraZoomDistance() _camera_zoom_distance: " + _camera_zoom_distance);

        // Record new cameraZoomDistance
        cameraZoomDistance = _camera_zoom_distance;
        
        // Set camera clip planes to be as tight as possible around the area being viewed, so as to maximize depth map resolution on mobile devices that seem to use 16-bit depth maps.
        SetCameraClippingPlanesForZoomDistance();
        
        // Determine the new alpha for nation labels and surround counts
        float new_surround_count_alpha = (_camera_zoom_distance <= SURROUND_COUNT_DISAPPEAR_START_ZOOM_DIST) ? 1f : ((_camera_zoom_distance >= SURROUND_COUNT_DISAPPEAR_END_ZOOM_DIST) ? 0f : ((SURROUND_COUNT_DISAPPEAR_END_ZOOM_DIST - _camera_zoom_distance) / (SURROUND_COUNT_DISAPPEAR_END_ZOOM_DIST - SURROUND_COUNT_DISAPPEAR_START_ZOOM_DIST)));
        new_surround_count_alpha *= SURROUND_COUNT_FULL_ALPHA;
        
        float new_nation_label_alpha = (_camera_zoom_distance <= NATION_LABEL_APPEAR_START_ZOOM_DIST) ? 0f : ((_camera_zoom_distance >= NATION_LABEL_APPEAR_END_ZOOM_DIST) ? 1f : ((_camera_zoom_distance - NATION_LABEL_APPEAR_START_ZOOM_DIST) / (NATION_LABEL_APPEAR_END_ZOOM_DIST - NATION_LABEL_APPEAR_START_ZOOM_DIST)));
        new_nation_label_alpha *= NATION_LABEL_FULL_ALPHA;

        float new_display_timer_alpha = (_camera_zoom_distance >= DISPLAY_TIMER_DISAPPEAR_END_ZOOM_DIST) ? 0f : ((_camera_zoom_distance <= DISPLAY_TIMER_DISAPPEAR_START_ZOOM_DIST) ? 1f : ((_camera_zoom_distance - DISPLAY_TIMER_DISAPPEAR_END_ZOOM_DIST) / (DISPLAY_TIMER_DISAPPEAR_START_ZOOM_DIST - DISPLAY_TIMER_DISAPPEAR_END_ZOOM_DIST)));
        new_display_timer_alpha *= DISPLAY_TIMER_FULL_ALPHA;
        
        // If the surround_count_alpha has changed, update the alpha value of all surround counts.
        if (new_surround_count_alpha != surround_count_alpha)
        {
            surround_count_alpha = new_surround_count_alpha;
            foreach (SurroundCount surround_count in active_surround_counts) {
                surround_count.SetAlpha(surround_count_alpha);
            }
        }

        // If the nation_label_alpha has changed, update the alpha value of all nation labels.
        if (new_nation_label_alpha != nation_label_alpha)
        {
            nation_label_alpha = new_nation_label_alpha;
            foreach (NationLabel nation_label in active_nation_labels) {
                nation_label.SetAlpha(nation_label_alpha);
            }
        }

        // If the display_timer_alpha has changed, update the alpha value of all display timers.
        if (new_display_timer_alpha != display_timer_alpha)
        {
            display_timer_alpha = new_display_timer_alpha;
            DisplayTimer.SetFullAlpha(display_timer_alpha);
        }

        // Update the ocean sound.
        UpdateSoundForZoomDistance();
    }

    public float GetCameraZoomDistance()
    {
        return cameraZoomDistance;
    }

    public void SetCameraClippingPlanesForZoomDistance()
    {
        // Set camera clip planes to be as tight as possible around the area being viewed, so as to maximize depth map resolution on mobile devices that seem to use 16-bit depth maps.
        camera.nearClipPlane = Math.Max(1, cameraZoomDistance * 0.6f - 50);// / 2f;
        camera.farClipPlane = cameraZoomDistance * 4f;
        overlayCamera.nearClipPlane = camera.nearClipPlane;
        overlayCamera.farClipPlane = camera.farClipPlane;
    }

    public void SetCameraClippingPlanesForMapPanel()
    {
        camera.nearClipPlane = Math.Max(1, cameraZoomDistance * 0.6f - 50);// / 2f;
        camera.farClipPlane = 1200f;
        overlayCamera.nearClipPlane = camera.nearClipPlane;
        overlayCamera.farClipPlane = camera.farClipPlane;
    }

    public bool LoadMapImage(int _serverID, int _mapID)
    {
        Debug.Log("LoadMapImage() called for _mapID " + _mapID);
        string filePath = Application.persistentDataPath + "/map_" + _serverID + "_" + _mapID + ".png"; 

        if (File.Exists(filePath) == false) {
            return false;
        }
         
        byte[] fileData = File.ReadAllBytes(filePath);
        Texture2D tex = new Texture2D(2, 2);
        bool result = tex.LoadImage(fileData); //..this will auto-resize the texture dimensions.

        //Debug.Log("LoadMapImage() file found with dims " + tex.width + "," + tex.height + ". Result: " + result + ". Path: " + filePath);

        if (result == false) {
            return false;
        }

        if ((tex.width <= 0) || (tex.height <= 0) || (tex.width > 10000) || (tex.height > 10000))
        {
            Debug.Log("Could not load map from file '" + filePath + "': dimensions are " + tex.width + "x" + tex.height + ".");
            return false;
        }

        // Initialize the map's ocean data array
        int width_in_chunks = (tex.width + CHUNK_SIZE - 1) / CHUNK_SIZE;
        int height_in_chunks = (tex.height + CHUNK_SIZE - 1) / CHUNK_SIZE;
        float[,] beach_count = new float[width_in_chunks, height_in_chunks];
        float[,] beach_density = new float[width_in_chunks, height_in_chunks];

        // Transfer the terrain data to a new array. Also flip the image vertically, because Unity's z axis is directed up.
        int cur_terrain;
        int[,] terrain = new int[tex.width, tex.height];
        Color32[] pix = tex.GetPixels32();
        for (int y = 0; y < tex.height; y++)
        {
            for (int x = 0; x < tex.width; x++)
            {
                cur_terrain = pix[((tex.height - y - 1) * tex.width) + x].b;

                // Sanity check the terrain value
                if ((cur_terrain < 0) || (cur_terrain >= TERRAIN_NUM_TYPES)) 
                {
                    Debug.Log("Server " + _serverID + ", map " + _mapID + " pos " + x + "," + y + " terrain out of bounds:" + cur_terrain);
                    cur_terrain = TERRAIN_DEEP_WATER;
                    Debug.Log("Could not load map from file '" + filePath + "': terrain value out of bounds. File may be corrupt.");
                    return false;
                }

                terrain[x, y] = cur_terrain;

                // Keep track of the number of beach squares in each of the map's chunks.
                if (cur_terrain == TERRAIN_BEACH) {
                    beach_count[x / CHUNK_SIZE, y / CHUNK_SIZE] += 1f;
                }
            }
        }

        // Normalize the beach data
        for (int y = 0; y < height_in_chunks; y++)
        {
            for (int x = 0; x < width_in_chunks; x++)
            {
                beach_count[x, y] = Math.Min(1f, beach_count[x, y] / (float)(CHUNK_SIZE));

                // Cut it off so that very small amounts of beach don't register as ocean.
                if (beach_count[x, y] < 0.25) {
                    beach_count[x, y] = 0f;
                }
            }
        }

        float BEACH_DENSITY_FALLOFF = 0.25f;
        for (int y = 0; y < height_in_chunks; y++)
        {
            for (int x = 0; x < width_in_chunks; x++)
            {
                beach_density[x, y] = beach_count[x, y];

                for (int i = 0; i < 4; i++)
                {
                    if (x >= i) beach_density[x, y] = Math.Max(beach_density[x, y], beach_count[x - i, y] * BEACH_DENSITY_FALLOFF);
                    if (x < (width_in_chunks - i)) beach_density[x, y] = Math.Max(beach_density[x, y], beach_count[x + i, y] * BEACH_DENSITY_FALLOFF);
                    if (y >= i) beach_density[x, y] = Math.Max(beach_density[x, y], beach_count[x, y - i] * BEACH_DENSITY_FALLOFF);
                    if (y < (height_in_chunks - i)) beach_density[x, y] = Math.Max(beach_density[x, y], beach_count[x, y + i] * BEACH_DENSITY_FALLOFF);
                }
            }
        }

        // Record the terrain array for this map in the map_terrains dictionary.
        map_terrains[_mapID] = terrain;

        // Record the beach density array for this map in the map_beach_densities dictionary.
        map_beach_densities[_mapID] = beach_density;

        // Record the map's dimensions vector in the map_dimension dictionary.
        map_dimensions[_mapID] = new Vector2Int(tex.width, tex.height);

        if (_mapID == GameData.MAINLAND_MAP_ID)
        {
            // Record the mainland's map center coords, for displaying map coords.
            mainlandMapCenterX = tex.width / 2;
            mainlandMapCenterZ = tex.height / 2;
        }

        return true;
    }

    public bool LoadEmblems()
	{
        string filePath = Application.persistentDataPath + "/emblems.png";

        Debug.Log("EmblemData.LoadEmblems() location: " + filePath);

        // Return false if the file doesn't exist on the client.
        if (File.Exists(filePath) == false) {
            return false;
        }

        byte[] fileData = File.ReadAllBytes(filePath);
        Texture2D tex = new Texture2D(2, 2);
        bool result = tex.LoadImage(fileData); //..this will auto-resize the texture dimensions.

        if (result == false) {
            return false;
        }

        emblemsPerRow = tex.width / EMBLEM_DIM;

        // Create the emblem data array.
        emblemData = new float[tex.width, tex.height];

        // Get the emblems image data.
        Color32[] pix = tex.GetPixels32();
        
        // Transfer the emblems image data into the emblemsData array.
        for (int y = 0; y < tex.height; y++)
        {
            for (int x = 0; x < tex.width; x++)
            {
                emblemData[x, y] = 1f - ((pix[((tex.height - y - 1) * tex.width) + x].r / 255f) * EMBLEM_INTENSITY);
            }
        }

        // Have the Customize panel init its list of emblems.
        CustomizePanel.instance.InitEmblems(tex, pix, emblemData);

        return true;
    }

    public void SetGraphicsQuality(GameGUI.GraphicsQuality _graphics_quality)
    {
        int max_terrain_lod = 0;

        switch (_graphics_quality)
        {
            case GameGUI.GraphicsQuality.BEST:
            case GameGUI.GraphicsQuality.GREAT:
                max_terrain_lod = 0;
                break;
            case GameGUI.GraphicsQuality.GOOD:
                max_terrain_lod = 1;
                break;
            case GameGUI.GraphicsQuality.FAST:
                max_terrain_lod = 2;
                break;
            case GameGUI.GraphicsQuality.FASTEST:
                max_terrain_lod = 3;
                break;
        }

        // Set max terrain LOD for all terrain patches.
        for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) 
        {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) 
            {
                // Set the terrain patch's max LOD.
                terrainPatches[x, z].terrain.heightmapMaximumLOD = max_terrain_lod;

                // If using the fastest LOD, move terrain to the TransparentFX layer, where the top-down light will shine on only the terrain.
                terrainPatches[x, z].terrain.gameObject.layer = (max_terrain_lod == 3) ? 1 : 0;
            }
        }

        // Only turn on the top-down light if we're using the fastest terrain LOD, which looks terrible under the normal directional light.
        topDownLight.gameObject.SetActive(max_terrain_lod == 3);
    }

    public IEnumerator SnapScreenshot()
    {
        int width = 1920;
        int height = 1280;

        Rect original_rect = camera.rect;
        camera.rect = new Rect(0, 0, 1, 1);

        // Create a RenderTexture and assign it to be the camera's render target.
        RenderTexture rt = new RenderTexture(width, height, 24, RenderTextureFormat.ARGB32);
        rt.antiAliasing = 8;
        rt.Create();
        camera.targetTexture = rt;
        
        // Render to the RenderTexture, and wait unto the frame is complete.
        camera.Render();
        yield return new WaitForEndOfFrame();

        // Make the RenderTexture active, so that ReadPixels will read from it rather than from the screen buffer.
        RenderTexture.active = rt;

        // Create a Texture2D and copy the image from the RenderTexture into it.
        Texture2D newTexture = new Texture2D(rt.width, rt.height);
        newTexture.ReadPixels(new Rect(0,0,rt.width, rt.height), 0,0);

        // Determine unused filename
        string file_path;
        for (int index = 1; ; index++)
        {
            file_path = Application.persistentDataPath + "/screenshot_" + index + ".png";
            if (File.Exists(file_path) == false) break;
        }

        // Save the image in the Texture2D to a png file.
        byte[] bytes = newTexture.EncodeToPNG();
        File.WriteAllBytes(file_path, bytes);

        // Clean up.
        camera.rect = original_rect;
        RenderTexture.active = null;
        camera.targetTexture = null;
        rt.Release();

        Debug.Log("Screenshot saved to " + file_path);
    }

    public void EnterMapPanelView(float _duration)
    {
        // Record that we're in map panel view
        mapPanelView = true;

        // Show all terrain patches, even those hidden because they're not in view.
        for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
                terrainPatches[x, z].terrain.gameObject.SetActive(true);
            }
        }

        // Record start and end camera position/rotation for this transition.
        Vector3 cameraPosStart = camera.transform.localPosition;
        Vector3 cameraRotStart = cameraAngle;
        Vector3 cameraPosEnd = new Vector3(((float)(viewDataBlockX0 + (VIEW_DATA_CHUNKS_WIDE * CHUNK_SIZE / 2)) + 0.5f) * BLOCK_SIZE, 1100f, -((float)(viewDataBlockZ0 + (VIEW_DATA_CHUNKS_WIDE * CHUNK_SIZE / 2)) + 0.5f) * BLOCK_SIZE);
        //Vector3 cameraPosEnd = new Vector3(((float)viewBlockX + 0.5f) * BLOCK_SIZE, 900f, -((float)viewBlockZ + 0.5f) * BLOCK_SIZE);
        Vector3 cameraRotEnd = new Vector3(90, 0, 0);

        // Perform the camera transition
        StartCoroutine(TransitionCamera(cameraPosStart, cameraRotStart, cameraPosEnd, cameraRotEnd, _duration, false));

        // Set the clip planes further out, for the map panel view.
        SetCameraClippingPlanesForMapPanel();
    }

    public void ExitMapPanelView(float _duration)
    {
        // Record start and end camera position/rotation for this transition.
        Vector3 cameraPosStart = camera.transform.localPosition;
        Vector3 cameraRotStart = new Vector3(90, 0, 0);
        Vector3 cameraPosEnd = GetCameraPositionForTargetBlock(viewBlockX, viewBlockZ);
        Vector3 cameraRotEnd = cameraAngle;

        if (_duration == 0f)
        {
            // Set camera to final state
            camera.transform.position = cameraPosEnd;
            camera.transform.eulerAngles = cameraRotEnd;
            SetCameraClippingPlanesForZoomDistance();
        }
        else
        {
            // Perform the camera transition
            StartCoroutine(TransitionCamera(cameraPosStart, cameraRotStart, cameraPosEnd, cameraRotEnd, _duration, true));
        }

        // Start coroutine to deactivate those terrain pathces that are not in view, once transition is complete.
        StartCoroutine(ExitMapPanelView_Coroutine(_duration));
    }

    public IEnumerator ExitMapPanelView_Coroutine(float _duration)
    {
        // Wait until the transition is complete
        yield return new WaitForSeconds(_duration);

        // Show only those terrain patches that are inside the map and in view, or else outside of the map.
        for (int z = 0; z < VIEW_DATA_CHUNKS_WIDE; z++) {
			for (int x = 0; x < VIEW_DATA_CHUNKS_WIDE; x++) {
                terrainPatches[x, z].terrain.gameObject.SetActive((terrainPatches[x, z].numBlocksInView > 0) || (terrainPatches[x, z].insideMap == false));
            }
        }

        // Record that we're no longer in map panel view
        mapPanelView = false;
    }

    public IEnumerator TransitionCamera(Vector3 _cameraPosStart, Vector3 _cameraRotStart, Vector3 _cameraPosEnd, Vector3 _cameraRotEnd, float _duration, bool _reset_clip_planes)
    {
        float start_time = Time.unscaledTime;
        float cur_time, progress;

        transitionInProgress = true;

        //Debug.Log("TransitionCamera() start pos: " + _cameraPosStart + ", start rot: " + _cameraRotStart + ", end pos: " + _cameraPosEnd + ", _cameraRotEnd: " + _cameraRotEnd);

        while ((cur_time = (Time.unscaledTime - start_time)) <= _duration)
        {
            // If the transition has ben interrupted, restore the camera angle and discontinue the transition.
            if (!transitionInProgress) 
            {
                camera.transform.eulerAngles = cameraAngle;
                yield break;
            }

            progress = cur_time / _duration;
            progress = Mathf.Pow(progress, .6f);
            camera.transform.position = Vector3.Lerp(_cameraPosStart, _cameraPosEnd, progress);
            Vector3 cur_rotation = Vector3.Slerp(_cameraRotStart, _cameraRotEnd, progress);
            camera.transform.eulerAngles = cur_rotation;

            //Debug.Log("TransitionCamera() progress: " + progress + ", cur_time: " + cur_time + ", cur_rotation: " + cur_rotation + ", camera.transform.rotation: " + camera.transform.rotation + ", _duration: " + _duration);
            yield return 0;
        }

        // Set camera to final state
        camera.transform.position = _cameraPosEnd;
        camera.transform.eulerAngles = _cameraRotEnd;

        // If necessary, reset camera clipping planes for camera zoom distance.
        if (_reset_clip_planes) {
            SetCameraClippingPlanesForZoomDistance();
        }

        transitionInProgress = false;
    }

    public MoveStep DetermineMoveStep(int _targetX, int _targetZ, bool _source_must_be_vacant)
    {
        //Debug.Log("DetermineMoveStep _targetX: " + _targetX + ", _targetZ: " + _targetZ);
        MoveStep result = new MoveStep();

        const int MOVE_STEP_MAX_RADIUS = 30;
        const int MOVE_STEP_MAX_ITERATIONS = 50;
        
        int minX = Math.Max(_targetX - MOVE_STEP_MAX_RADIUS, viewDataBlockX0);
        int maxX = Math.Min(_targetX + MOVE_STEP_MAX_RADIUS, viewDataBlockX1);
        int minZ = Math.Max(_targetZ - MOVE_STEP_MAX_RADIUS, viewDataBlockZ0);
        int maxZ = Math.Min(_targetZ + MOVE_STEP_MAX_RADIUS, viewDataBlockZ1);
        int x, z;
        BlockData blockData;

        // Get the target block's data.
        blockData = GetBlockData(_targetX, _targetZ);

        // Return invalid result if the target block doesn't exist, or is occupied by a nation.
        if ((blockData == null) || (blockData.nationID != -1)) {
            return result;
        }

        // Return invalid result if the target block cannot be occupied.
        if (!BlockCanBeOccupied(_targetX, _targetZ, true)) {
            return result;
        }

        // Initialize the process_data of the target block (to the number of steps it is away from the target block).
        blockData.process_data = 0;

        // Initialize process_data of all blocks within the max radius.
        for (z = minZ; z <= maxZ; z++)
        {
            for (x = minX; x <= maxX; x++)
            {
                // Skip the target block.
                if ((x == _targetX) && (z == _targetZ)) {
                    continue;
                }

                blockData = GetBlockData(x, z);

                if (blockData != null) 
                {
                    if (((blockData.nationID != -1) && (blockData.nationID != GameData.instance.nationID)) || (!BlockCanBeOccupied(x, z, true))) {
                        // The block either belongs to another nation or cannot be occupied. It cannot be part of any path.
                        blockData.process_data = MoveStep.INVALID_STEP_COUNT;
                    } else {
                        // The block belongs to the player's nation or no nation, and is habitable. It can be part of a path.
                        blockData.process_data = MoveStep.MAX_STEP_COUNT;
                    }
                }
            }
        }

        int extentX0 = _targetX - 1;
        int extentX1 = _targetX + 1;
        int extentZ0 = _targetZ - 1;
        int extentZ1 = _targetZ + 1;
        int nextExtentX0, nextExtentX1, nextExtentZ0, nextExtentZ1;
        bool path_expanded, source_found = false;
        int step_count, best_step_count, best_step_x, best_step_z;

        for (int i = 0; i < MOVE_STEP_MAX_ITERATIONS; i++)
        {
            // Reset path_expanded so as to be able to tell if it is exapnded at all during this iteration.
            path_expanded = false;

            nextExtentX0 = extentX0;
            nextExtentX1 = extentX1;
            nextExtentZ0 = extentZ0;
            nextExtentZ1 = extentZ1;

            for (z = extentZ0; z <= extentZ1; z++)
            {
                for (x = extentX0; x <= extentX1; x++)
                {
                    // Get the current block's data.
                    blockData = GetBlockData(x, z);

                    // Skip this block if it doesn't exist.
                    if (blockData == null) {
                        continue;
                    }

                    // Skip this block if a step count has already been determined for it (or if it was dtermined to be invlid for use in paths above).
                    if (blockData.process_data != MoveStep.MAX_STEP_COUNT) {
                        continue;
                    }

                    best_step_count = MoveStep.MAX_STEP_COUNT;
                    best_step_x = -1;
                    best_step_z = -1;

                    step_count = DetermineMoveStep_GetNeighborStepCount(x, z - 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x; best_step_z = z - 1; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x - 1, z);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x - 1; best_step_z = z; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x + 1, z);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x + 1; best_step_z = z; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x, z + 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x; best_step_z = z + 1; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x - 1, z - 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x - 1; best_step_z = z - 1; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x + 1, z - 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x + 1; best_step_z = z - 1; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x - 1, z + 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x - 1; best_step_z = z + 1; }

                    step_count = DetermineMoveStep_GetNeighborStepCount(x + 1, z + 1);
                    if (step_count < best_step_count) { best_step_count = step_count; best_step_x = x + 1; best_step_z = z + 1; }

                    // If this block is along a potential path...
                    if (best_step_count < MoveStep.MAX_STEP_COUNT)
                    {
                        //Debug.Log("Pos " + x + "," + z + " best_step_count: " + best_step_count + ", best_step_x: " + best_step_x + ", best_step_z: " + best_step_z);
                        
                        // Determine this block's step count.
                        step_count = best_step_count + 1;

                        // Only allow the block to be part of the path if the block doesn't belong to the player's nation. If it does, it can be the source square, but cannot be along the path.
                        if (blockData.nationID != GameData.instance.nationID)
                        {
                            // Set this block's process_data to its best step count.
                            blockData.process_data = step_count;
                       
                            // Expand the extent (for the next iteration) for this block, if necessary.
                            nextExtentX0 = Math.Max(minX, Math.Min(nextExtentX0, x - 1));
                            nextExtentX1 = Math.Min(maxX, Math.Max(nextExtentX1, x + 1));
                            nextExtentZ0 = Math.Max(minZ, Math.Min(nextExtentZ0, z - 1));
                            nextExtentZ1 = Math.Min(maxZ, Math.Max(nextExtentZ1, z + 1));

                            // Record that the path has been expanded during this iteration.
                            path_expanded = true;
                        }

                        // If this block meets the criteria for being the source of the step, record that the source has been found.
                        if ((blockData.nationID == GameData.instance.nationID) && ((!_source_must_be_vacant) || (blockData.objectID == -1)))
                        {
                            // Only replace the previous result if this new result has a lower step count.
                            if (step_count < result.step_count)
                            {
                                result.valid = true;
                                result.x0 = x;
                                result.z0 = z;
                                result.x1 = best_step_x;
                                result.z1 = best_step_z;
                                result.step_count = step_count;
                                source_found = true;
                            }
                        }
                    }
                }
            }

            // If the path has not been expanded any further in this latest iteration, there's no more progress to be made. Stop iterating.
            if (!path_expanded) {
                break;
            }

            // If we've found a source square during this latest iteration, we're all done. Stop iterating.
            if (source_found) {
                break;
            }

            extentX0 = nextExtentX0;
            extentX1 = nextExtentX1;
            extentZ0 = nextExtentZ0;
            extentZ1 = nextExtentZ1;
        }

        //Debug.Log("DetermineMoveStep() returning " + result.valid + ", from " + result.x0 + "," + result.z0 + " to " + result.x1 + "," + result.z1);

        return result;
    }

    public bool BlockCanBeOccupied(int _x, int _z, bool _check_boundaries)
    {
        // Determine whether the current block's terrain is habitable.
        int terrain = MapView.instance.GetBlockTerrain(_x, _z);

        // Get the block's data
        BlockData block_data = GetBlockData(_x, _z);

        // If the terrain is uninhabitable, return false. Note that whether it has an object on it is checked, because some resource objects appear on terrain types that would otherwise be uninhabitable.
        // Note also that TERRAIN_BEACH is habitable on mainland and raidland, but not on homeland (so that raiders can move around on the beach).
        if (((terrain != MapView.TERRAIN_BEACH) || (GameData.instance.mapMode == GameData.MapMode.HOMELAND)) && (terrain != MapView.TERRAIN_FLAT_LAND) && (block_data.objectID == -1)) {
            return false;
        }

        // If the terrain is affected by a lasting wipe that prohibits this nation from entering, return false.
        if ((block_data.wipe_end_time > Time.time) && (((block_data.wipe_flags & BuildData.WIPE_FLAG_GENERAL) != 0) || (((block_data.wipe_flags & BuildData.WIPE_FLAG_SPECIFIC) != 0) && (block_data.wipe_nationID == gameData.nationID)))) {
            return false;
        }

        if (_check_boundaries && (GameData.instance.mapMode == GameData.MapMode.MAINLAND))
        {
            // If the block is west of the nation's western boundary, return false.
            if ((_x <= GameData.instance.map_position_limit) && (!GameData.instance.userIsAdmin)) {
                return false;
            }

            // If the block is east of the nation's eastern boundary, return false.
            if ((_x >= GameData.instance.map_position_eastern_limit) && (!GameData.instance.userIsAdmin)) {
                return false;
            }

            // If the user or nation is a veteran, and the block is south of the veteran area boundary, return false.
            if ((GameData.instance.userIsVeteran || GameData.instance.nationIsVeteran) && (GameData.instance.newPlayerAreaBoundary != -1) && (_z >= GameData.instance.newPlayerAreaBoundary) && (!GameData.instance.userIsAdmin)) {
                return false;
            }

            // If the block is outside of the nation's maximum range extent, return false.
            if (((_x <= MapView.instance.mainlandMaxExtentX0) ||
                 (_x >= MapView.instance.mainlandMaxExtentX1) ||
                 (_z <= MapView.instance.mainlandMaxExtentZ0) ||
                 (_z >= MapView.instance.mainlandMaxExtentZ1)) &&
                (!GameData.instance.userIsAdmin))
            {
                return false;
            }
        }

        return true;
    }

    public int DetermineMoveStep_GetNeighborStepCount(int _x, int _z)
    {
        BlockData blockData = GetBlockData(_x, _z);
        return (blockData == null) ? MoveStep.INVALID_STEP_COUNT : blockData.process_data;
    }

    public bool IsEligibleForAutoProcess(AutoProcessType _type, int _blockX, int _blockZ)
    {
        BlockData blockData;

        if ((_type == AutoProcessType.EVACUATE) || (_type == AutoProcessType.OCCUPY))
        {
            for (int z = _blockZ - 2; z <= _blockZ + 2; z++)
            {
                for (int x = _blockX - 2; x <= _blockX + 2; x++)
                {
                    blockData = GetBlockData(x, z);

                    // Skip if this block is not represented on the client.
                    if (blockData == null) {
                        continue;
                    }

                    // Skip if this block is currently locked.
                    if (blockData.locked_until > Time.unscaledTime) {
                        continue;
                    }
                
                    if (_type == AutoProcessType.EVACUATE)
                    {
                        if (blockData.nationID == gameData.nationID) {
                            return true;
                        }
                    }
                    else if (_type == AutoProcessType.OCCUPY)
                    {
                        // If this block is empty and can be occupied...
                        if ((blockData.nationID == -1) && BlockCanBeOccupied(x, z, true))
                        {
                            // If there is a path to move to this block...
                            MoveStep move_step_from_vacant = DetermineMoveStep(x, z, false);
                            if (move_step_from_vacant.valid) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        if (_type == AutoProcessType.OCCUPY)
        {
            MoveStep move_step = DetermineMoveStep(_blockX, _blockZ, false);
            if (move_step.valid) {
                return true;
            }
        }

        if (_type == AutoProcessType.MOVE_TO)
        {
            MoveStep move_step_from_vacant = DetermineMoveStep(_blockX, _blockZ, true);
            if (move_step_from_vacant.valid) {
                return true;
            }
        }

        return false;
    }

    public AutoProcessType GetAutoProcess()
    {
        return autoProcess;
    }

    public void StopAutoProcess()
    {
        autoProcess = AutoProcessType.NONE;
    }

    public void StartAutoProcess(AutoProcessType _type, int _blockX, int _blockZ)
    {
        //Debug.Log("StartAutoProcess type: " + _type);
        autoProcess = _type;
        autoProcessBlockX = _blockX;
        autoProcessBlockZ = _blockZ;
        autoProcessMoveX = _blockX;
        autoProcessMoveZ = _blockZ;
        autoProcessEvacX = -1;
        autoProcessEvacZ = -1;
        autoProcessStartTime = Time.unscaledTime;
        autoProcessLastMoveStepX = -1;
        autoProcessLastMoveStepZ = -1;
        autoProcessPrevAttemptTime = 0f;
        autoProcessFirstStep = true;

        // If starting an occupy process, determine square to move to before the occupy can begin. This may not be the center square of the process, which may be inaccessible.
        if (_type == AutoProcessType.OCCUPY)
        {
            for (int z = _blockZ - 2; z <= _blockZ + 2; z++)
            {
                for (int x = _blockX - 2; x <= _blockX + 2; x++)
                {
                    BlockData blockData = GetBlockData(x, z);

                    // Skip if this block is not represented on the client.
                    if (blockData == null) {
                        continue;
                    }

                    // Skip if this block is currently locked.
                    if (blockData.locked_until > Time.unscaledTime) {
                        continue;
                    }
                
                    // If this block is empty and can be occupied...
                    if ((blockData.nationID == -1) && BlockCanBeOccupied(x, z, true))
                    {
                        // If there is a path to move to this block...
                        MoveStep move_step_from_vacant = DetermineMoveStep(x, z, false);
                        if (move_step_from_vacant.valid) 
                        {
                            autoProcessMoveX = x;
                            autoProcessMoveZ = z;
                            break;
                        }
                    }
                }
            }
        }

    }

    public void ContinueAutoProcess()
    {
        //Debug.Log("ContinueAutoProcess() center: " + autoProcessBlockX + "," + autoProcessBlockZ + ", evac: " + autoProcessEvacX + "," + autoProcessEvacZ);
        BlockData blockData;

        if ((Time.unscaledTime - autoProcessStartTime) > AUTO_PROCESS_MAX_TIME) 
        {
            // This auto process has timed out, abort it.
            StopAutoProcess();
            return;
        }

        autoProcessPrevAttemptTime = Time.unscaledTime;

        for (int z = autoProcessBlockZ - 2; z <= autoProcessBlockZ + 2; z++)
        {
            for (int x = autoProcessBlockX - 2; x <= autoProcessBlockX + 2; x++)
            {
                blockData = GetBlockData(x, z);

                // Skip if this block is not represented on the client.
                if (blockData == null) {
                    continue;
                }

                // Skip if this block is currently locked.
                if (blockData.locked_until > Time.unscaledTime) {
                    continue;
                }
                
                if (autoProcess == AutoProcessType.EVACUATE)
                {
                    if (blockData.nationID == gameData.nationID)
                    {
                        // Send an evacuate event to the server.
                        Network.instance.SendCommand("action=evacuate|x=" + x + "|y=" + z + "|auto=" + (autoProcessFirstStep ? 0 : 1));
                        //Debug.Log("Sent auto evac message at time " + Time.unscaledTime);

                        // Record this latest step taken.
                        autoProcessPrevStepTime = Time.unscaledTime;

                        // Record that the first step has been taken.
                        autoProcessFirstStep = false;

                        // Record that this user event has occurred
                        GameData.instance.UserEventOccurred(GameData.UserEventType.EVACUATE);

                        return;
                    }
                }
                else if (autoProcess == AutoProcessType.OCCUPY)
                {
                    if ((blockData.nationID == -1) && IsBlockAdjacentToNation(x, z))
                    {
                        if (BlockCanBeOccupied(x, z, true))
                        {
                            // Send a mapclick event to the server.
                            Network.instance.SendCommand("action=mapclick|x=" + x + "|y=" + z + "|auto=" + (autoProcessFirstStep ? 0 : 1));
                            //Debug.Log("Sent auto occupy message at time " + Time.unscaledTime);

                            // Record this latest step taken.
                            autoProcessPrevStepTime = Time.unscaledTime;

                            // Record that the first step has been taken.
                            autoProcessFirstStep = false;

                            // Record that this user event has occurred
                            GameData.instance.UserEventOccurred(GameData.UserEventType.OCCUPY);

                            return;
                        }
                    }
                }
            }
        }

        // Evac the previous step's source square if necessary.
        if (autoProcessEvacX != -1)
        {
            // Send an evacuate event to the server.
            Network.instance.SendCommand("action=evacuate|x=" + autoProcessEvacX + "|y=" + autoProcessEvacZ + "|auto=1");
            //Debug.Log("Sent request to evac block " + autoProcessEvacX + "," + autoProcessEvacZ + " at time " + Time.unscaledTime);

            // Reset the record of the source square to evac.
            autoProcessEvacX = -1;
            autoProcessEvacZ = -1;

            // Record that this user event has occurred
            GameData.instance.UserEventOccurred(GameData.UserEventType.EVACUATE);

            return;
        }

        if ((autoProcess == AutoProcessType.OCCUPY) || (autoProcess == AutoProcessType.MOVE_TO))
        {
            MoveStep step = DetermineMoveStep(autoProcessMoveX, autoProcessMoveZ, (autoProcess == AutoProcessType.MOVE_TO));

            if (step.valid)
            {
                blockData = GetBlockData(step.x1, step.z1);

                // If the block is not locked, take the step into it.
                if (blockData.locked_until < Time.unscaledTime)
                {
                    // Send a mapclick event to the server.
                    Network.instance.SendCommand("action=mapclick|x=" + step.x1 + "|y=" + step.z1 + "|auto=" + (autoProcessFirstStep ? 0 : 1));
                    //Debug.Log("Sent auto step message at time " + Time.unscaledTime);

                    // If this is a move_to process, or an occupy process later than the first step, evac the source square as the next action.
                    if ((autoProcessFirstStep == false) || (autoProcess == AutoProcessType.MOVE_TO))
                    {
                        autoProcessEvacX = step.x0;
                        autoProcessEvacZ = step.z0;
                        //Debug.Log("Set evac block to " + autoProcessEvacX + "," + autoProcessEvacZ + " for step to " + step.x1 + "," + step.z1);
                    }

                    // Record this latest step taken.
                    autoProcessPrevStepTime = Time.unscaledTime;
                    autoProcessLastMoveStepX = step.x1;
                    autoProcessLastMoveStepZ = step.z1;

                    // Record that the first step has been taken.
                    autoProcessFirstStep = false;

                    // Record that this user event has occurred
                    GameData.instance.UserEventOccurred(GameData.UserEventType.OCCUPY);

                    return;
                }

                return;
            }
        }

        // If step(s) were first moved as part of an occupy area process, evac the final step moved if it is outside the area to occupy.
        if ((autoProcess == AutoProcessType.OCCUPY) && (autoProcessLastMoveStepX != -1) && 
            ((autoProcessLastMoveStepX < (autoProcessBlockX - 2)) || (autoProcessLastMoveStepX > (autoProcessBlockX + 2)) || (autoProcessLastMoveStepZ < (autoProcessBlockZ - 2)) || (autoProcessLastMoveStepZ > (autoProcessBlockZ + 2))))
        {
            // Send an evacuate event to the server.
            Network.instance.SendCommand("action=evacuate|x=" + autoProcessLastMoveStepX + "|y=" + autoProcessLastMoveStepZ + "|auto=1");
            //Debug.Log("Sent final evacuate message at time " + Time.unscaledTime);

            // Reset the record of the last step taken.
            autoProcessLastMoveStepX = -1;
            autoProcessLastMoveStepZ = -1;

            // Record that this user event has occurred
            GameData.instance.UserEventOccurred(GameData.UserEventType.EVACUATE);

            return;
        }

        // If no successful steps in this autoprocess have been taken in a while... 
        if ((Time.unscaledTime - autoProcessPrevStepTime) > 10f)
        {
            // There are no more blocks that fit the criteria for this automatic process. End the process.
            StopAutoProcess();
        }

        //Debug.Log("AutoProcess ending no more blocks meet criteria.");
    }

    public void ShowSmallSelectionSquare(int _x0, int _z0, int _x1, int _z1)
    {
        smallSelectionSquare.transform.position = new Vector3(_x0 * BLOCK_SIZE, LAND_LEVEL, -_z0 * BLOCK_SIZE);
        smallSelectionSquare.transform.localScale = new Vector3(_x1 - _x0 + 1, 1, _z1 - _z0 + 1);
        smallSelectionSquare.GetComponent<SelectionSquare>().Show();
    }

    public void ShowLargeSelectionSquare(int _x0, int _z0, int _x1, int _z1)
    {
        largeSelectionSquare.transform.position = new Vector3(_x0 * BLOCK_SIZE, LAND_LEVEL, -_z0 * BLOCK_SIZE);
        largeSelectionSquare.transform.localScale = new Vector3(_x1 - _x0 + 1, 1, _z1 - _z0 + 1);
        largeSelectionSquare.GetComponent<SelectionSquare>().Show();
    }

    public void HideSelectionSquares()
    {
        smallSelectionSquare.GetComponent<SelectionSquare>().Hide();
        largeSelectionSquare.GetComponent<SelectionSquare>().Hide();
    }

    public void UpdateReplay()
    {
        // Do nothing if the replay is paused.
        if ((Time.timeScale == 0f) || (GameData.instance.replayCurTime >= GameData.instance.replayEndTime)) {
            return;
        }

        // Update the replay's current time.
        // Note: sometimes, mysteriously, the assignment part of this line slows down replay framerate in the editor.
        GameData.instance.replayCurTime += Time.deltaTime;
        
        while ((GameData.instance.replayEventIndex < GameData.instance.replayList.Count) && (GameData.instance.replayList[GameData.instance.replayEventIndex].timestamp <= GameData.instance.replayCurTime))
        {
            // Get the current reply event record.
            ReplayEventRecord record = GameData.instance.replayList[GameData.instance.replayEventIndex];

            //Debug.Log("Replaying event: " + record.eventID + " for " + record.x + "," + record.z);

            if ((record.eventID == ReplayEventRecord.Event.SET_NATION_ID) || (record.eventID == ReplayEventRecord.Event.CLEAR_NATION_ID) || (record.eventID == ReplayEventRecord.Event.BATTLE))
            {
                int oldNationID = blocksReplay[record.x, record.z].nationID;
                int newNationID = ((record.eventID == ReplayEventRecord.Event.SET_NATION_ID) || (record.eventID == ReplayEventRecord.Event.BATTLE)) ? record.subjectID : -1;
                int battle_flags = (record.eventID == ReplayEventRecord.Event.BATTLE) ? record.battle_flags : 0;

                // Update the raid score header for the modification of this block.
                UpdateReplay_BlockModified(record.x, record.z, oldNationID, newNationID);
                
                // Modify the block's nationID.
                blocksReplay[record.x, record.z].nationID = newNationID;

                // Run a block process to display the modification.
                int process_type = (newNationID == -1) ? BlockProcess.PROCESS_EVACUATE : ((oldNationID == -1) ? BlockProcess.PROCESS_OCCUPY : BlockProcess.PROCESS_BATTLE);
                InitBlockProcess(record.x, record.z, process_type, newNationID, 0.4f, 1, 1, 1, battle_flags);
            }
            else if (record.eventID == ReplayEventRecord.Event.TOWER_ACTION)
            {
                foreach (TargetRecord target in record.targets)
                {
                    // Update the raid score header for the modification of this block.
                    UpdateReplay_BlockModified(target.x, target.y, blocksReplay[target.x, target.y].nationID, target.newNationID);

                    // Modify the block's nationID.
                    blocksReplay[target.x, target.y].nationID = target.newNationID;
                }

                // Initialize a tower action to display the modifications.
                InitTowerAction(record.x, record.z, record.subjectID, (BuildData.Type)record.build_type, record.invisible_time, record.duration, record.trigger_x, record.trigger_z, record.triggerNationID, record.targets);
            }
            else if (record.eventID == ReplayEventRecord.Event.EXT_DATA)
            {
                float cur_time = Time.time;

                // Record the replay block's extended data.
                BlockData replayBlockData = blocksReplay[record.x, record.z];
                replayBlockData.objectID = record.subjectID;
                replayBlockData.owner_nationID = record.owner_nationID;
                replayBlockData.creation_time = record.creation_time + cur_time;
                replayBlockData.completion_time = (record.completion_time == -1f) ? -1f : (record.completion_time + cur_time);
                replayBlockData.invisible_time = (record.invisible_time == -1f) ? -1f : (record.invisible_time + cur_time);
                replayBlockData.capture_time = record.capture_time + cur_time;
                replayBlockData.crumble_time = (record.crumble_time == -1f) ? -1f : (record.crumble_time + cur_time);
                replayBlockData.wipe_nationID = record.wipe_nationID;
                replayBlockData.wipe_end_time = (record.wipe_end_time == -1f) ? -1f : (record.wipe_end_time + cur_time);
                replayBlockData.wipe_flags = record.wipe_flags;

                // Copy the replay blocks extended data into the current view's equivalent block.
                BlockData blockData = GetBlockData(record.x, record.z);
                if (blockData != null)
                {
                    blockData.objectID = replayBlockData.objectID;
                    blockData.owner_nationID = replayBlockData.owner_nationID;
                    blockData.creation_time = replayBlockData.creation_time;
                    blockData.completion_time = replayBlockData.completion_time;
                    blockData.invisible_time = replayBlockData.invisible_time;
                    blockData.capture_time = replayBlockData.capture_time;
                    blockData.crumble_time = replayBlockData.crumble_time;
                    blockData.wipe_nationID = replayBlockData.wipe_nationID;
                    blockData.wipe_end_time = replayBlockData.wipe_end_time;
                    blockData.wipe_flags = replayBlockData.wipe_flags;
                }

                // The block's extended data has been modified.
                ModifiedExtendedData(record.x, record.z);
            }
            else if (record.eventID == ReplayEventRecord.Event.COMPLETE)
            {
                // This block's object is being completed.
                CompleteBuildObject(record.x, record.z);
            }
            else if (record.eventID == ReplayEventRecord.Event.SALVAGE)
            {
                // This block's object is being salvaged.
                SalvageBuildObject(record.x, record.z);
            }
            else if (record.eventID == ReplayEventRecord.Event.TRIGGER_INERT)
            {
                // Attempt has been made to trigger this block's inert build.
                InitTriggerInert(record.x, record.z);
            }

            // Advance index to the next replay event record.
            GameData.instance.replayEventIndex++;
        }

        if (GameData.instance.replayCurTime >= GameData.instance.replayEndTime) {
            GameData.instance.replayCurTime = GameData.instance.replayEndTime;
        }
    }

    public void UpdateReplay_BlockModified(int _x, int _z, int _oldNationID, int _newNationID)
    {
        bool updateScoreHeader = false;
        int oldRaidFlags = GameData.instance.raidFlags;
        int newRaidFlags = oldRaidFlags;

        // If the defending nation has lost area, decrement its area count and update the raid score header.
        if ((_oldNationID == GameData.instance.raidDefenderNationID) && (_newNationID != GameData.instance.raidDefenderNationID))
        {
            GameData.instance.raidDefenderArea--;
            updateScoreHeader = true;
        }

        // If the defending nation has gained area, increment its area count and update the raid score header.
        if ((_oldNationID != GameData.instance.raidDefenderNationID) && (_newNationID == GameData.instance.raidDefenderNationID))
        {
            GameData.instance.raidDefenderArea++;
            updateScoreHeader = true;
        }

        // Red Shard
        if (blocksReplay[_x, _z].objectID == 200) { 
            newRaidFlags = (newRaidFlags & ~(int)GameData.RaidFlags.RED_SHARD) | (((_newNationID != GameData.instance.raidDefenderNationID) && (_newNationID != -1)) ? (int)GameData.RaidFlags.RED_SHARD : 0);
        }

        // Green Shard
        if (blocksReplay[_x, _z].objectID == 201) { 
            newRaidFlags = (newRaidFlags & ~(int)GameData.RaidFlags.GREEN_SHARD) | (((_newNationID != GameData.instance.raidDefenderNationID) && (_newNationID != -1)) ? (int)GameData.RaidFlags.GREEN_SHARD : 0);
        }

        // Blue Shard
        if (blocksReplay[_x, _z].objectID == 202) { 
            newRaidFlags = (newRaidFlags & ~(int)GameData.RaidFlags.BLUE_SHARD) | (((_newNationID != GameData.instance.raidDefenderNationID) && (_newNationID != -1)) ? (int)GameData.RaidFlags.BLUE_SHARD : 0);
        }

        // 50%
        if (GameData.instance.raidDefenderArea <= (GameData.instance.raidDefenderStartingArea / 2)) {
            newRaidFlags = newRaidFlags | (int)GameData.RaidFlags.PROGRESS_50_PERCENT;
        } else {
            newRaidFlags = newRaidFlags & ~(int)GameData.RaidFlags.PROGRESS_50_PERCENT;
        }

        // 100%
        if (GameData.instance.raidDefenderArea == 0) {
            newRaidFlags = newRaidFlags | (int)GameData.RaidFlags.PROGRESS_100_PERCENT;
        } else {
            newRaidFlags = newRaidFlags & ~(int)GameData.RaidFlags.PROGRESS_100_PERCENT;
        }
                
        // If the raid flags have changed, update the score header.
        if (newRaidFlags != oldRaidFlags)
        {
            GameData.instance.raidFlags = newRaidFlags;
            updateScoreHeader = true;
        }

        // Update the score header if necessary.
        if (updateScoreHeader) {
            RaidScoreHeader.instance.UpdateDisplay(true);
        }
    }

    public void UpdateSoundForLocalOceanDensity()
    {
        // Do nothing if there is not currently a map.
        if (mapBeachDensity == null) {
            return;
        }

        int chunk_x0 = ((viewBlockX - HALF_CHUNK_SIZE) / CHUNK_SIZE) - ((viewBlockX < HALF_CHUNK_SIZE) ? 1 : 0);
        int chunk_x1 = Math.Min(Math.Max(chunk_x0 + 1, 0), mapChunkDimX - 1);
        chunk_x0 = Math.Min(Math.Max(chunk_x0, 0), mapChunkDimX - 1);
        int chunk_z0 = ((viewBlockZ - HALF_CHUNK_SIZE) / CHUNK_SIZE) - ((viewBlockZ < HALF_CHUNK_SIZE) ? 1 : 0);
        int chunk_z1 = Math.Min(Math.Max(chunk_z0 + 1, 0), mapChunkDimZ - 1);
        chunk_z0 = Math.Min(Math.Max(chunk_z0, 0), mapChunkDimZ - 1);

        // Determine degree of transition between chunk_x0 and chunk_x1, and between chunk_z0 and chunk_z1
        float x_degree = (float)(viewBlockX - (((int)((viewBlockX - HALF_CHUNK_SIZE) / CHUNK_SIZE) - ((viewBlockX < HALF_CHUNK_SIZE) ? 1 : 0)) * CHUNK_SIZE + HALF_CHUNK_SIZE)) / (float)CHUNK_SIZE;
        float z_degree = (float)(viewBlockZ - (((int)((viewBlockZ - HALF_CHUNK_SIZE) / CHUNK_SIZE) - ((viewBlockZ < HALF_CHUNK_SIZE) ? 1 : 0)) * CHUNK_SIZE + HALF_CHUNK_SIZE)) / (float)CHUNK_SIZE;

        // Determine ocean density at camera postion, by interpolating between the values for the 4 closest chunks.
        float ocean_density_z0 = (mapBeachDensity[chunk_x0, chunk_z0] * (1f - x_degree)) + (mapBeachDensity[chunk_x1, chunk_z0] * x_degree);
        float ocean_density_z1 = (mapBeachDensity[chunk_x0, chunk_z1] * (1f - x_degree)) + (mapBeachDensity[chunk_x1, chunk_z1] * x_degree);
        float ocean_density = (ocean_density_z0 * (1f - z_degree)) + (ocean_density_z1 * z_degree);
        
        // Set the volume of the ocean sound.
        Sound.instance.SetOceanVolume(ocean_density);

        //Debug.Log("bx: " + viewBlockX + ", x0: " + chunk_x0 + ", x1: " + chunk_x1 + ", z0: " + chunk_z0 + ", z1: " + chunk_z1 + ", x_deg: " + x_degree + ", z_degree: " + z_degree + ", dens: " + ocean_density);
    }

    public void UpdateSoundForZoomDistance()
    {
        float cam_y = camera.gameObject.transform.position.y;

        float zoom_volume_multiplier = 1;

        if (cam_y > 200) {
            zoom_volume_multiplier *= (1f - Math.Min(1, ((cam_y - 120) / 450)));
        }

        Sound.instance.SetZoomVolumeMultiplier(zoom_volume_multiplier);
    }

    // Update the terrain textures for the part of the world being viewed, if necessary.
    public void UpdateTerrainForViewPosition()
    {
        Color[] key1 = null, key2 = null;
        float lerp_degree = 0f;

        // Determine terrain position, bsed on the map's skin. Skin 0 (used for the mainland map) varies terrain from east to west; other maps use the same terrain throughout.
        float new_terrain_position = 0f;
        switch (mapSkin)
        {
            case 0: new_terrain_position = Math.Min(1f, Math.Max(0f, (float)(((int)(viewBlockX / TERRAIN_POSITION_QUANTUM)) * TERRAIN_POSITION_QUANTUM) / (float)(mapDimX - 1))); break;
            case 1: new_terrain_position = 0.1f; break;
            case 2: new_terrain_position = 0.4f; break;
            case 3: new_terrain_position = 0.6f; break;
            case 4: new_terrain_position = 0.9f; break;
            default: new_terrain_position = 0f; break;
        }

        // If the quantized terrain position is unchanged, do nothing.
        if (new_terrain_position == terrain_position) {
            return;
        }

        // Record the new terrain position.
        terrain_position = new_terrain_position;

        // Tell the sound system where in the environment we are, for ambient sound probabilities.
        for (int i = 0; i < transitionPositions.Length; i++)
        {
            if (transitionPositions[i] > terrain_position)
            {
                float position_between = (terrain_position - transitionPositions[i - 1]) / (transitionPositions[i] - transitionPositions[i - 1]);
                if (i <= 1) Sound.instance.SetAmbientAreaProbabilities(1f, 0f, 0f, 0f);
                else if (i == 2) Sound.instance.SetAmbientAreaProbabilities(1f - position_between, position_between, 0f, 0f);
                else if (i == 3) Sound.instance.SetAmbientAreaProbabilities(0f, 1f, 0f, 0f);
                else if (i == 4) Sound.instance.SetAmbientAreaProbabilities(0f, 1f - position_between, position_between, 0f);
                else if (i == 5) Sound.instance.SetAmbientAreaProbabilities(0f, 0f, 1f, 0f);
                else if (i == 6) Sound.instance.SetAmbientAreaProbabilities(0f, 0f, 1f - position_between, position_between);
                else if (i >= 7) Sound.instance.SetAmbientAreaProbabilities(0f, 0f, 0f, 1f);
                break;
            }
        }

        // Determine the texture2 modification values for the current terrain position.
        Vector4 prev_texture2_vals = texture2ModVals[0];
        foreach (Vector4 element in texture2ModVals)
        {
            if (element.w <= terrain_position) 
            {
                prev_texture2_vals = element;
            }
            else 
            {
                Vector3 mod_vals_1 = new Vector3(prev_texture2_vals.x, prev_texture2_vals.y, prev_texture2_vals.z);
                Vector3 mod_vals_2 = new Vector3(element.x, element.y, element.z);

                key1 = GetTextureKey(texture2_keys, terrainTexture2, texture2Pixels, mod_vals_1);
                key2 = GetTextureKey(texture2_keys, terrainTexture2, texture2Pixels, mod_vals_2);

                lerp_degree = (terrain_position - prev_texture2_vals.w) / (element.w - prev_texture2_vals.w);
                Vector3 mod_vals_lerp = Vector3.Lerp(mod_vals_1, mod_vals_2, lerp_degree);

                if (texture2CurModVals.Equals(mod_vals_lerp) == false)
                {
                    terrainTexture2Copy.SetPixels(GetTextureLerp(mod_vals_lerp, texture2_lerps_vals, texture2_lerps, terrainTexture2, key1, key2, lerp_degree));
                    terrainTexture2Copy.Apply();
                    texture2CurModVals = mod_vals_lerp;
                    //Debug.Log("Applied texture2");
                }
                break;
            }
        }

        // Determine the texture3 modification values for the current terrain position.
        Vector4 prev_texture3_vals = texture3ModVals[0];
        foreach (Vector4 element in texture3ModVals)
        {
            //Debug.Log("texture3ModVals element: " + element);
            if (element.w <= terrain_position) 
            {
                prev_texture3_vals = element;
            }
            else 
            {
                Vector3 mod_vals_1 = new Vector3(prev_texture3_vals.x, prev_texture3_vals.y, prev_texture3_vals.z);
                Vector3 mod_vals_2 = new Vector3(element.x, element.y, element.z);

                key1 = GetTextureKey(texture3_keys, terrainTexture3, texture3Pixels, mod_vals_1);
                key2 = GetTextureKey(texture3_keys, terrainTexture3, texture3Pixels, mod_vals_2);

                lerp_degree = (terrain_position - prev_texture3_vals.w) / (element.w - prev_texture3_vals.w);
                Vector3 mod_vals_lerp = Vector3.Lerp(mod_vals_1, mod_vals_2, lerp_degree);

                if (texture3CurModVals.Equals(mod_vals_lerp) == false)
                {
                    terrainTexture3Copy.SetPixels(GetTextureLerp(mod_vals_lerp, texture3_lerps_vals, texture3_lerps, terrainTexture3, key1, key2, lerp_degree));
                    terrainTexture3Copy.Apply();
                    texture3CurModVals = mod_vals_lerp;
                    //Debug.Log("Applied texture3");
                }
                break;
            }
        }

        /*
        // TESTING -- For manually trying out new color modification values.
        Vector3 texture2Mods = new Vector3(texture2HueMod, texture2SatMod, texture2ValMod);
        Color[] texture2Key = GetTextureKey(texture2_keys, terrainTexture2, texture2Pixels, texture2Mods);
        terrainTexture2Copy.SetPixels(texture2Key);
        terrainTexture2Copy.Apply();
        Vector3 texture3Mods = new Vector3(texture3HueMod, texture3SatMod, texture3ValMod);
        Color[] texture3Key = GetTextureKey(texture3_keys, terrainTexture3, texture3Pixels, texture3Mods);
        terrainTexture3Copy.SetPixels(texture3Key);
        terrainTexture3Copy.Apply();
        */

        // Set the water color for the current terrain position.
        float prev_water_color_pos = 0f;
        for (int i = 0; i < waterColorPositions.Length; i++)
        {
            if ((i == 0) || (waterColorPositions[i] < terrain_position))
            {
                prev_water_color_pos = waterColorPositions[i];
            }
            else 
            {
                Color32 water_color = Color32.Lerp(waterColorVals[Math.Max(0, i - 1)], waterColorVals[i], (terrain_position - prev_water_color_pos) / (waterColorPositions[i] - prev_water_color_pos));
                //Debug.Log("Water color: " + water_color);
                waterTile1Rend.material.SetColor("_BaseColor", water_color);
                waterTile2Rend.material.SetColor("_BaseColor", water_color);
                waterTile3Rend.material.SetColor("_BaseColor", water_color);
                waterTile4Rend.material.SetColor("_BaseColor", water_color);
                break;
            }
        }
    }

    public Color[] GetTextureKey(Dictionary<Vector3, Color[]> _dictionary, Texture2D _original_texture, Color[] _original_pixels, Vector3 _modification)
    {
        // If the given texture's key corresponding to the given modification already exists, return it.
        if (_dictionary.ContainsKey(_modification)) {
            return _dictionary[_modification];
        }

        // Create the new key and record it in the dictionary.
        Color[] pixels = ChangeHSB(_original_pixels, _modification.x, _modification.y, _modification.z);
        _dictionary[_modification] = pixels;

        // Return the new key.
        return pixels;
    } 

    public Color[] GetTextureLerp(Vector3 _mod_vals, List<Vector3> _lerps_vals, List<Color[]> _lerps, Texture2D _original_texture, Color[] _key1, Color[] _key2, float _lerp_degree)
    {
        // Return the lerp corresponding to the given _mod_vals, if it already exists in the given cache.
        for (int i = 0; i < _lerps_vals.Count; i++)
        {
            if (_lerps_vals[i] == _mod_vals) 
            {
                // If this lerp is not already at the end of the list, move it to the end of the list so it will not be removed soon.
                if (i < (_lerps.Count - 1))
                {
                    _lerps_vals.Add(_mod_vals);
                    _lerps.Add(_lerps[i]);

                    _lerps_vals.RemoveAt(i);
                    _lerps.RemoveAt(i);
                }

                // Return the lerp at the end of the list (the one that has been found to match the given _mod_vals).
                return _lerps[_lerps.Count - 1];
            }
        }

        //Debug.Log("CREATING NEW LERP");

        // The given lerp doesn't exist in the cache; create it and add it to the end of the cache.
        Color[] result = LerpTexture(_key1, _key2, _lerp_degree);
        _lerps_vals.Add(_mod_vals);
        _lerps.Add(result);

        // If the cache has exceeded its max size, remove its first (oldest) element.
        if (_lerps_vals.Count > TERRAIN_LERP_CACHE_SIZE)
        {
            _lerps_vals.RemoveAt(0);
            _lerps.RemoveAt(0);
        }

        // Return the new lerp.
        return result;
    }

    public Color[] LerpTexture(Color[] _key1, Color[] _key2, float _lerp_degree)
    {
        float source_degree = 1.0f - _lerp_degree;
	    Color[] resultPixels = new Color[_key1.Length];
        
   	    for (int i = 0; i < _key1.Length; i++) {
		    resultPixels[i] = (_key1[i] * source_degree) + (_key2[i] * _lerp_degree);
	    }	
	
	    return resultPixels;
    }

    public Color[] ChangeHSB(Color[] _source, float _hue, float _saturation, float _brightness)
    {
	    Color[] resultPixels = new Color[_source.Length];
        Vector4 hsba;
	
	    for (int i = 0; i < _source.Length; i++) 
	    {
		    hsba = ColorToHSBA(_source[i]);
		    hsba.x += _hue;
		    hsba.y += _saturation;
		    hsba.z += _brightness;
		    resultPixels[i] = HSBAtoColor(hsba);
	    }	
	
	    return resultPixels;
    }

    //----------------------------------------------------------------------------------------------------------------------------------------------------------  
    // Convert Color to HSBA (Vector4)
    public Vector4 ColorToHSBA (Color _color)
    {
        float minValue = Mathf.Min(_color.r, Mathf.Min(_color.g, _color.b));
        float maxValue = Mathf.Max(_color.r, Mathf.Max(_color.g, _color.b));
        float delta = maxValue - minValue;
        // hsba
        Vector4 result = new Vector4 (0, 0, maxValue, _color.a);
        
        // Calculate the HUE in degrees
        if (maxValue == _color.r) 
        {
	        if (_color.g >= _color.b)  result.x  =  (delta == 0)  ?  0  :   60.0f * (_color.g - _color.b) / delta;
			else if(_color.g < _color.b) result.x = 60.0f * (_color.g - _color.b) / delta + 360;
        }
        else if (maxValue == _color.g)
        { 
            result.x = 60.0f * (_color.b - _color.r) / delta + 120;
        }
        else if (maxValue == _color.b)
        {	
            result.x = 60.0f * (_color.r - _color.g) / delta + 240;
        }
        result.x /= 360;

        // Calculate saturation 
        result.y  =  (maxValue == 0)  ?  0  :  1.0f - (minValue / maxValue);
    
        return result;
    }


    //----------------------------------------------------------------------------------------------------------------------------------------------------------  
    // Convert HSBA(Vector4) to Color
    public Color HSBAtoColor(Vector4 hsba)
    {
        Color result;
    
        // if Saturation > 0
        if(hsba.y > 0) 
	    {  
            // Calculate sector
    	    float secPos = (hsba.x * 360.0f) / 60.0f;
    	    int secNr = Mathf.FloorToInt(secPos);
    	    float secPortion = secPos - secNr;

    	    // Calculate axes
    	    float p = hsba.z * (1.0f - hsba.y);
    	    float q = hsba.z * (1.0f - (hsba.y * secPortion));
    	    float t = hsba.z * (1.0f - (hsba.y * (1.0f - secPortion)));
            
    	    // Calculate RGB
    	    switch (secNr) 
	         {
	           case 1:
	           result = new Color(q, hsba.z, p, hsba.w);
	       	    break;
	       	
	           case 2:
	       	    result = new Color(p, hsba.z, t, hsba.w);
	       	    break;
	       	
	           case 3:
	       	    result = new Color(p, q, hsba.z, hsba.w);
	       	    break;
	       	
	           case 4:
	       	    result = new Color(t, p, hsba.z, hsba.w);
	       	    break;
	       	
	           case 5:
	       	    result = new Color(hsba.z, p, q, hsba.w); 
	       	    break;
	       	
	           default:
	       	    result = new Color(hsba.z, t, p, hsba.w);
	       	    break;
	        }
        }
        else 
        {
            result = new Color(hsba.z, hsba.z, hsba.z, hsba.w);
        }
    
        return result;
    }
}

public class BlockData
{
	public int nationID = -1;
    public int label_nationID = -1;
    public float locked_until = 0.0f;
    public int flags = 0;

    // Extended data
   	public int objectID = -1;
	public int owner_nationID = -1;
    public float creation_time = -1;
	public float completion_time = -1;
	public float invisible_time = -1;
    public float capture_time = -1;
	public float crumble_time = -1;
	public int wipe_nationID = -1;
	public float wipe_end_time = -1;
    public int wipe_flags = 0;

    public int target_x, target_z;

    // Representation of 3D object within this block.
    public BuildObject build_object = null;
    public LandscapeObject landscape_object = null;

    // Representation of limit barriers within this block
    public GameObject limit_object = null;
    public GameObject vet_limit_object = null;
    public GameObject extent_limit_object = null;
    public GameData.LimitType limit_object_type = GameData.LimitType.Undef;
    public GameData.LimitType vet_limit_object_type = GameData.LimitType.Undef;
    public GameData.LimitType extent_limit_object_type = GameData.LimitType.Undef;

    // Object representing the block's lasting wipe.
    public GameObject wipe_object = null;

    // Object displaying the count of surrounding enemy towers.
    public GameObject surround_count = null;

    // Record of whether this block is in the view area.
    public bool in_view_area = false;

    // Used temporarily for processes.
    public int process_data = 0;

    public void Clear()
    {
        nationID = -1;
        label_nationID = -1;
        locked_until = 0.0f;
        flags = 0;
        objectID = -1;
        wipe_end_time = -1;
        in_view_area = false;
        target_x = target_z = -1;

        if (build_object != null)
        {
            build_object.CleanUp();
            build_object = null;
        }

        if (landscape_object != null)
        {
            landscape_object.CleanUp();
            landscape_object = null;
        }

        // Remove any limit boundry lines.
        MapView.instance.RemoveBoundaryLines(this);

        // Remove any max extent limit boundry lines.
        MapView.instance.RemoveMaxExtentLines(this);

        // If the block has a surround count, remove it.
        if (surround_count != null) {
            MapView.instance.RemoveSurroundCount(this);
        }
    }

    public void Copy(BlockData _original)
    {
        nationID = _original.nationID;
        label_nationID = -1;
        locked_until = 0.0f;
        flags = 0;

        // Extended data
   	    objectID = _original.objectID;
	    owner_nationID = _original.owner_nationID;
        creation_time = _original.creation_time;
	    completion_time = _original.completion_time;
	    invisible_time = _original.invisible_time;
        capture_time = _original.capture_time;
	    crumble_time = _original.crumble_time;
	    wipe_nationID = _original.wipe_nationID;
	    wipe_end_time = _original.wipe_end_time;
        wipe_flags = _original.wipe_flags;
    }
}

public class PatchUpdate {
	public int chunkX, chunkZ;
	public int singleBlockX, singleBlockZ;
    public bool setHeights, fullUpdateRequested;
}

public class IntVector2 {
    public IntVector2(int _x, int _z) { x = _x;  z = _z; }
    public IntVector2(IntVector2 _original) { x = _original.x; z = _original.z; }
    public bool Equals(IntVector2 _compare) { return ((x == _compare.x) && (z == _compare.z)); }
    public int x;
    public int z;

    public static IntVector2 operator +(IntVector2 v1, IntVector2 v2) {
        return new IntVector2(v1.x + v2.x, v1.z + v2.z);
    }
}

public class MoveStep
{
    public const int MAX_STEP_COUNT = 1000000;
    public const int INVALID_STEP_COUNT = 2000000;

    public bool valid = false;
	public int x0 = -1, z0 = -1, x1 = -1, z1 = -1, step_count = MAX_STEP_COUNT;
}


