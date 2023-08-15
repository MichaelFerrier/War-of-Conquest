using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class BuildObject : MonoBehaviour
{
    private const float TRANSITION_DURATION = 1.5f;
    private const float WALL_FADE_DURATION = 1.0f;
    private const float FADE_AND_CLEAN_UP_DURATION = 1.0f;
    private const float CAPTURED_ALPHA = 0.4f;

    private const float MIN_LOOK_ALIVE_PERIOD = 3f;
    private const float MAX_LOOK_ALIVE_PERIOD = 40f;
    private const float IDLE_PERIOD = 25f;

    public BuildData build_data = null;
    int blockX, blockZ;
    int objectID = -1;
    BlockData block_data = null;
    bool wall = false, wall_east = false, wall_south = false, wall_north = false, wall_west = false, wall_post = true, wall_run = false;
    float prev_activate_time = -1;
    float salvage_time = -1;

    float inert_start_time = -1;
    float inert_end_time = -1;
    bool inert = false;

    float invisible_start_time = -1;
    float invisible_end_time = -1;
    bool invisible = false;

    bool objects_instantiated = false;
    GameObject object0 = null;
    GameObject object1 = null;
    GameObject object2 = null;
    GameObject construction = null;

    List<GameObject> fadingObjectsList = new List<GameObject>();

    BuildAppearance cur_appearance = null;
    BuildAppearance transition_end_appearance = null;
    float next_update_time = 0f;

    GameObject raid_prefab = null, raid_object = null;
    public GameObject storageMeter = null;

    GameObject timerText = null;
    float prevUpdateTimerTextTime = 0f;

    DisplayTimer displayTimer = null;

    // Normal: Albedo white with full alpha. No emission.
    // Incomplete: Albedo white with partial alpha. No emission. Construction object shown.
    // Captured: Albedo black with partial alpha. No emission.
    // Inert: Albedo blue with partial alpha. No emission. 
    Color albedo_normal      = new Color(1f, 1f, 1f, 1f);
    Color albedo_incomplete  = new Color(1f, 1f, 1f, 0.4f);
    Color albedo_captured    = new Color(0f, 0f, 0f, CAPTURED_ALPHA);
    Color albedo_inert       = new Color(0f, 0f, 1f, 1f);
    Color albedo_invisible   = new Color(1f, 1f, 1f, 0f);

    Color albedo_start_construction = new Color(1f, 1f, 1f, 0f);
    Color albedo_end_salvage = new Color(1f, 1f, 1f, 0f);
    Color albedo_end_crumble = new Color(0f, 0f, 0f, 0f);

    Color emission_normal = new Color(0f, 0f, 0f, 0f);
    Color emission_start_construction = new Color(1f, 1f, 1f, 1f); 
    Color emission_end_salvage = new Color(1f, 1f, 1f, 1f);

    public void OnDisable()
    {
        //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " OnDisable(), fadingObjectsList contains " + fadingObjectsList.Count);

        // Destroy each object that was in the process of fading out, since the fade coroutine will no longer be able to run now that this is disabled.
        foreach (GameObject gameObject in fadingObjectsList)
        {
            ObjectData.DestroyAllMaterials(gameObject);
            Object.Destroy(gameObject);
        }

        // Clear the fadingObjectsList.
        fadingObjectsList.Clear();
    }

    public void Initialize(int _blockX, int _blockZ)
    {
        // Cache data about this block and object.
        blockX = _blockX;
        blockZ = _blockZ;
        block_data = MapView.instance.GetBlockData(_blockX, _blockZ);
        objectID = block_data.objectID;
        build_data = BuildData.GetBuildData(objectID);

        // TESTING
        if (build_data == null) Debug.Log("BuildObject.Initialize() build_data NULL for ID " + objectID);

        wall = build_data.type == BuildData.Type.WALL;

        // If the block is set to become invisible in the future, update its invisibility at that time.
        if (block_data.invisible_time > Time.time) {
            StartCoroutine(UpdateInvisibilityAfterDelay(block_data.invisible_time - Time.time));
        }

        if ((block_data.nationID == GameData.instance.nationID) && (block_data.completion_time > Time.time)) {
            InitTimerText();
        }

        // Initialize storage meter.
        if ((block_data.completion_time <= Time.time) && RequiresStorageMeter()) {
            InitStorageMeter();
        }

        // Display the display timer if appropriate.
        UpdateDisplayTimer();

        //Debug.Log("Initialize() object " + objectID + " belonging to nation " + block_data.owner_nationID + " at block " + _blockX + "," + _blockZ + " of nation " + block_data.nationID + ". invisible time: " + block_data.invisible_time + ", cur tie: " + Time.time);

        // If this is a wall segment, determine adjacent walls.
        if (wall) {
            DetermineAdjacentWalls();
        }

        // Determine whether this object should be invisible.
        SetInvisible(DetermineWhetherInvisible(), false);

        //Debug.Log("Object at " + blockX + "," + blockZ + " invisible time: " + block_data.invisible_time + ", cur time: " + Time.time + ", invisible: " + invisible + ", block nation: " + block_data.nationID);

        if (!invisible)
        {
            // Instantiate the 3D objects associated with this build.
            InstantiateObjects();
        }

        if (!wall)
        {
            if ((build_data.type == BuildData.Type.DIRECTED_MULTIPLE) || (build_data.type == BuildData.Type.SPLASH) ||
                ((build_data.type == BuildData.Type.RECAPTURE) && (build_data.original_name.Contains("Ecto Ray"))) ||
                (build_data.original_name.Contains("SatCom")))
            {
                // If this block has no target block location, generate a random starting target block location.
                if ((block_data.target_x == -1) && (block_data.target_z == -1)) {
                    SetRandomTargetBlock(_blockX, _blockZ, block_data);
                }

                // Instantly change aim to point at the determined target block.
                Aim(block_data.target_x, block_data.target_z, 0f);

                // Initialize occassional random changes in aim, to keep the object looking alive and active.
                StartCoroutine(LookAlive());
            }
            else if ((build_data.type == BuildData.Type.AIR_DROP) || (build_data.type == BuildData.Type.GENERAL_LASTING_WIPE))
            {
                // Initialize repeatedly playing idle animation, to keep the object looking alive and active.
                StartCoroutine(Idle());
            }
        }

        // Perform initial update. Do this immediately so that the object is not rendered before the first update.
        PerformCurrentUpdate();

        // If this is a wall, update any adjacent walls for the addition of this wall.
        if (wall) UpdateAdjacents();

        // If appropriate, play construction sound.
        if (block_data.creation_time == Time.time) {
            Sound.instance.PlayInWorld(Sound.instance.construction, MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ));
        }
    }

    public bool IsInert()
    {
        return inert;
    }

    public void SetInert(bool _inert, bool _transition)
    {
        // If this object is already set to the given inert state, do nothing.
        if (inert == _inert) {
            return;
        }

        // Record new inert state.
        inert = _inert;

        // Record inert transition time.
        if (_inert)
        {
            inert_start_time = Time.time - (_transition ? 0 : TRANSITION_DURATION);
            inert_end_time = -1;
        }
        else
        {
            inert_start_time = -1;
            inert_end_time = Time.time - (_transition ? 0 : TRANSITION_DURATION);
        }

        //Debug.Log("SetInert(" + _inert + "," + _transition + "), inert_start_time: " + inert_start_time);

        if (_transition)
        {
            // Stop any transition that might already be in progress.
            transition_end_appearance = null;
        }

        // Update to initiate the inert transition.
        PerformCurrentUpdate();
    }

    public void UpdateDisplayTimer()
    {
        //Debug.Log("UpdateDisplayTimer() called for " + blockX + "," + blockZ);
        if (block_data == null)
        {
            if (displayTimer != null)
            {
                // Deactivate the displayTimer.
                DisplayTimer.Deactivate(displayTimer.gameObject);
                displayTimer = null;
            }

            return;
        }

        bool show_crumble_timer = (block_data.crumble_time != -1) && (block_data.crumble_time > Time.time);
        bool show_rebuild_timer = (build_data.trigger_on != BuildData.TriggerOn.UNDEF) && (block_data.nationID == block_data.owner_nationID) && ((block_data.capture_time + GameData.instance.defenseRebuildPeriod) > Time.time);

        //Debug.Log("show_crumble_timer: " + show_crumble_timer + ", block_data.crumble_time: " + block_data.crumble_time); 
        //Debug.Log("UpdateDisplayTimer() for " + blockX + "," + blockZ + ", show_crumble_timer: " + show_crumble_timer + ", block_data.crumble_time: " + block_data.crumble_time + ", Time.time: " + Time.time);

        if (show_crumble_timer)
        {
            float end_time = block_data.crumble_time;
            float start_time = block_data.capture_time;

            if ((displayTimer == null) || (!DisplayTimer.IsActive(displayTimer.gameObject))) {
                displayTimer = DisplayTimer.ActivateNew(blockX, blockZ, DisplayTimer.Type.CRUMBLE, start_time, end_time);
            } else {
                displayTimer.SetType(DisplayTimer.Type.CRUMBLE);
                displayTimer.SetTimes(start_time, end_time);
            }
        }
        else if (show_rebuild_timer)
        {
            float end_time = block_data.capture_time + GameData.instance.defenseRebuildPeriod;
            float start_time = block_data.capture_time;

            if ((displayTimer == null) || (!DisplayTimer.IsActive(displayTimer.gameObject))) {
                displayTimer = DisplayTimer.ActivateNew(blockX, blockZ, DisplayTimer.Type.ACTIVATE, start_time, end_time);
            } else {
                displayTimer.SetType(DisplayTimer.Type.ACTIVATE);
                displayTimer.SetTimes(start_time, end_time);
            }
        }
        else
        {
            if (displayTimer != null)
            {
                // Deactivate the displayTimer.
                DisplayTimer.Deactivate(displayTimer.gameObject);
                displayTimer = null;
            }
        }
    }

    public void CleanUpDisplayTimer()
    {
        if (displayTimer != null)
        {
            // Deactivate the displayTimer.
            DisplayTimer.Deactivate(displayTimer.gameObject);
            displayTimer = null;
        }
    }

    public bool IsInvisible()
    {
        return invisible;
    }

    public void SetInvisible(bool _invisible, bool _transition)
    {
        // If this object is already set to the given visibility state, do nothing.
        if (invisible == _invisible) {
            return;
        }

        // Record new visibility state.
        invisible = _invisible;

        if ((!invisible) && (!objects_instantiated))
        {
            // Instantiate the 3D objects associated with this build.
            InstantiateObjects();
        }

        // Record invisible transition time.
        if (_invisible)
        {
            invisible_start_time = Time.time - (_transition ? 0 : TRANSITION_DURATION);
            invisible_end_time = -1;
        }
        else
        {
            invisible_start_time = -1;
            invisible_end_time = Time.time - (_transition ? 0 : TRANSITION_DURATION);
        }

        //Debug.Log("SetInvisible(" + _invisible + "," + _transition + "), invisible_start_time: " + invisible_start_time);

        if (_transition)
        {
            // Stop any transition that might already be in progress.
            transition_end_appearance = null;
        }

        // Update to initiate the visibility transition.
        PerformCurrentUpdate();
    }

    public bool DetermineWhetherInvisible()
    {
        // If this build object is no longer associated with a block, just return current invisibility state.
        if (block_data == null) {
            return invisible;
        }

        if (GameData.instance.mapMode == GameData.MapMode.REPLAY) 
        {
            // In a replay, the block's object should not be invisible if it is occupied by the raid's attacker nation.
            if (block_data.nationID == GameData.instance.raidAttackerNationID) {
                return false;
            }
        }
        else 
        {
            // The block's object should not be invisible if it is occupied by the player's nation.
            if (block_data.nationID == GameData.instance.nationID) {
                return false;
            }
        }

        //Debug.Log("DetermineWhetherInvisible() block_data.invisible_time: " + block_data.invisible_time + ", Time.time: " + Time.time + ", block_data.owner_nationID: " + block_data.owner_nationID + ", block_data.nationID: " + block_data.nationID + ", GameData.instance.nationID: " + GameData.instance.nationID);
        return (block_data.invisible_time != -1) && (block_data.invisible_time <= Time.time) && (block_data.nationID == block_data.owner_nationID);
    }

    public void UpdateInvisibility()
    {
        //Debug.Log("UpdateInvisibility() block_data.invisible_time: " + block_data.invisible_time + ", Time.time: " + Time.time + " invisible: " + DetermineWhetherInvisible());

        if (block_data == null) {
            return; // This object has been removed from the map since the coroutine that is calling this was initiated.
        }

        // Set the new appropriate visibility state, transitioning to it if it has changed.
        SetInvisible(DetermineWhetherInvisible(), true);

        // If the block is set to become invisible in the future, update its invisibility at that time.
        if (block_data.invisible_time > Time.time) {
            StartCoroutine(UpdateInvisibilityAfterDelay(block_data.invisible_time - Time.time));
        }
    }

    public void InitStorageMeter()
    {
        storageMeter = StorageMeter.Get(build_data, blockX, blockZ);
    }

    public void CleanUpStorageMeter()
    {
        if (storageMeter != null)
        {
            StorageMeter.Release(storageMeter);
            storageMeter = null;
        }
    }

    public bool RequiresStorageMeter()
    {
        return ((build_data.type == BuildData.Type.ENERGY_STORAGE) || (build_data.type == BuildData.Type.MANPOWER_STORAGE) || ((build_data.type == BuildData.Type.SHARD) && (block_data.owner_nationID == GameData.instance.nationID)));
    }

    public void InitTimerText()
    {
        // Set up a new MapText object to represent this object's timer.
        timerText = MemManager.instance.GetMapTextObject();
        timerText.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ) + new Vector3(0, 0, -1);
        timerText.transform.localEulerAngles = new Vector3(90f, 45f, 0f);
        timerText.SetActive(true);
        TextMesh text_mesh = timerText.transform.GetChild(0).GetComponent<TextMesh>();
        text_mesh.text = "";
        timerText.GetComponent<GUITransition>().StartTransition(0,1,1,1,false);
    }

    public void CleanUpTimerText()
    {
        if (timerText != null)
        {
            // Remove and release the timer text.
            //timerText.SetActive(false);
            timerText.GetComponent<GUITransition>().StartTransition(1,0,1,1,true);
            MemManager.instance.ReleaseMapTextObject(timerText);
            timerText = null;
        }
    }

    public IEnumerator UpdateInvisibilityAfterDelay(float _delay)
    {
        // After the given delay...
        yield return new WaitForSeconds(_delay + 0.1f);

        // ...update this object's invisibility.
        UpdateInvisibility();
    }

    public void Salvage()
    {
        // Record the time at which the salvage takes place.
        salvage_time = Time.time;

        // Stop any transition that might already be in progress.
        transition_end_appearance = null;

        // Update to initiate the salvage transition.
        PerformCurrentUpdate();

        // Remove the block_data, as it is no longer valid.
        block_data = null;
    }

    public void Complete()
    {
        // Stop any transition that might already be in progress.
        transition_end_appearance = null;

        // Remove the timer text.
        CleanUpTimerText();

        // Display the storage meter if appropriate
        if (RequiresStorageMeter()) {
            InitStorageMeter();
        }

        // Update to initiate the completion transition.
        PerformCurrentUpdate();
    }

    public void Captured()
    {
        // Stop any transition that might already be in progress.
        transition_end_appearance = null;

        // Display the crumble timer if appropriate.
        UpdateDisplayTimer();

        // Update to initiate the capture transition.
        PerformCurrentUpdate();
    }

    public void UpdateForChangeToAdjacent()
    {
        if (wall)
        {
            // Cache old record of whether there are connecting walls to east or south.
            bool prev_wall_east = wall_east;
            bool prev_wall_south = wall_south;
            bool prev_wall_post = wall_post;

            // Redetermine adjacent connecting walls.
            DetermineAdjacentWalls();

            if ((prev_wall_east != wall_east) || (prev_wall_south != wall_south) || (prev_wall_post != wall_post))
            {
                bool captured = (block_data != null) && (block_data.nationID != block_data.owner_nationID);
                float full_alpha = captured ? CAPTURED_ALPHA : 1f;

                //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " UpdateForChangeToAdjacent(), prev_wall_east: " + prev_wall_east + ", wall_east: " + wall_east + ", object0: " + object0 + ", object1: " + object1 + ", object2: " + object2);

                // If the wall post is to disappear, fade it out.
                if (prev_wall_post && !wall_post)
                {
                    StartCoroutine(FadeObject(object0, full_alpha, 0f, WALL_FADE_DURATION));
                    object0 = null;
                }

                // If the east wall segment is to disappear, fade it out.
                if (prev_wall_east && !wall_east)
                {
                    //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " UpdateForChangeToAdjacent() about to fade out object 1.");
                    StartCoroutine(FadeObject(object1, full_alpha, 0f, WALL_FADE_DURATION));
                    object1 = null;
                }

                // If the south wall segment is to disappear, fade it out.
                if (prev_wall_south && !wall_south)
                {
                    StartCoroutine(FadeObject(object2, full_alpha, 0f, WALL_FADE_DURATION));
                    object2 = null;
                }

                // Re-instantiate the objects for the new configuration.
                DeleteObjects();
                InstantiateObjects();

                // Apply this object's current appearance.
                BuildAppearance appearance = DetermineBuildAppearance(Time.time);
                ApplyBuildAppearance(appearance);

                // If the wall post segment is to appear, fade it in.
                if (!prev_wall_post && wall_post) {
                    StartCoroutine(FadeObject(object0, 0f, full_alpha, WALL_FADE_DURATION));
                }

                // If the east wall segment is to appear, fade it in.
                if (!prev_wall_east && wall_east) {
                    //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " UpdateForChangeToAdjacent() about to fade in object 1.");
                    StartCoroutine(FadeObject(object1, 0f, full_alpha, WALL_FADE_DURATION));
                }

                // If the south wall segment is to appear, fade it in.
                if (!prev_wall_south && wall_south) {
                    StartCoroutine(FadeObject(object2, 0f, full_alpha, WALL_FADE_DURATION));
                }
            }
        }
    }

    public IEnumerator FadeObject(GameObject _wall_object, float _start_alpha, float _end_alpha, float _duration)
    {
        float start_time = Time.time;
        float end_time = Time.time + _duration;
        Color fade_object_albedo = new Color();
        fade_object_albedo.r = cur_appearance.object_albedo.r;
        fade_object_albedo.g = cur_appearance.object_albedo.g;
        fade_object_albedo.b = cur_appearance.object_albedo.b;

        if (_end_alpha == 0f)
        {
            // Add the given object to the fading objects list.
            fadingObjectsList.Add(_wall_object);
        }

        // Fade the wall segment in or out over time. 
        // Check for timeout at end of loop, so that final iteration >= end time occurs, to set final alpha value.
        do {
            yield return null;

            // Exit coroutine if the _wall_object has been destroyed.
            if (_wall_object == null) {
                yield break;
            }

            // Set the given object's appearance to reflect the current status of its fade.
            fade_object_albedo.a = (Mathf.Max(0f, Time.time - start_time) / _duration * (_end_alpha - _start_alpha)) + _start_alpha;
            ApplyAppearanceRecursively(_wall_object, fade_object_albedo, cur_appearance.object_emission);
        } while (Time.time <= end_time);

        // If the wall segment has faded out, destroy it.
        if (_end_alpha == 0f) 
        {
            fadingObjectsList.Remove(_wall_object);
            ObjectData.DestroyAllMaterials(_wall_object);
            Object.Destroy(_wall_object);
        }
    }

    public void FadeAndCleanUp()
    {
        StartCoroutine(FadeAndCleanUp_Coroutine());
    }

    public IEnumerator FadeAndCleanUp_Coroutine()
    {
        block_data = null;

        if (object0 != null) {
            StartCoroutine(FadeObject(object0, 1f, 0f, FADE_AND_CLEAN_UP_DURATION));
        }

        if (object1 != null) {
            StartCoroutine(FadeObject(object1, 1f, 0f, FADE_AND_CLEAN_UP_DURATION));
        }

        if (object2 != null) {
            StartCoroutine(FadeObject(object2, 1f, 0f, FADE_AND_CLEAN_UP_DURATION));
        }

        if (construction != null) {
            StartCoroutine(FadeObject(construction, 1f, 0f, FADE_AND_CLEAN_UP_DURATION));
        }

        yield return new WaitForSeconds(FADE_AND_CLEAN_UP_DURATION);

        CleanUp();
    }

    public void CleanUp()
    {
        //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " here3");

        // If this is a wall, update any adjacent walls for the removal of this wall.
        if (wall) UpdateAdjacents();

        // Delete the 3D objects associated with this build.
        DeleteObjects();

        CleanUpTimerText();

        CleanUpStorageMeter();

        CleanUpDisplayTimer();

        if (construction != null) 
        {
            ObjectData.DestroyAllMaterials(construction);
            Object.Destroy(construction);
            construction = null;
        }

        // If this is a wall, update any adjacent walls for the removal of this wall.
        if (wall) {
            UpdateAdjacents();
        }

        // Destroy this GameObject.
        Destroy(gameObject);
    }

    void Update()
    {
        // Update this object's timer text if appropriate.
        if ((block_data != null) && (timerText != null) && ((Time.time - prevUpdateTimerTextTime) > 0.5f)) {
            TextMesh text_mesh = timerText.transform.GetChild(0).GetComponent<TextMesh>();
            text_mesh.text = GameData.instance.GetDurationShortText((int)Mathf.Ceil(block_data.completion_time - Time.time));
        }

        // If this object does not need to be updated currently, return.
        if ((next_update_time == -1f) || (next_update_time > Time.time)) {
            return;
        }

        // Perform update.
        PerformCurrentUpdate();
    }

    void OnDestroy()
    {
        if (raid_object != null) { ObjectData.DestroyAllMaterials(raid_object); Object.Destroy(raid_object); }
    }

    private void PerformCurrentUpdate()
    {
        // If a transition is taking place, apply appearance for current position in transition.
        if (transition_end_appearance != null)
        {
            //Debug.Log("Performing transition...");

            BuildAppearance appearance = BuildAppearance.Lerp(cur_appearance, transition_end_appearance, Time.time);
            //Debug.Log("Updating, time: " + Time.time + ", end time: " + transition_end_appearance.cur_time + ", object albedo a: " + appearance.object_albedo.a);
            ApplyBuildAppearance(appearance);

            if (Time.time >= transition_end_appearance.cur_time)
            {
                // We've reached the end of this transition.
                cur_appearance = appearance;

                // If the block contains an object that has finished crumbling...
                if ((block_data != null) && (block_data.objectID != -1) && (block_data.nationID != block_data.owner_nationID) && (block_data.crumble_time != -1) && (block_data.crumble_time <= Time.time))
                {
                    // Remove the object from the block.
                    block_data.objectID = -1;
                    block_data.build_object = null;

                    // Set this BuildObject's block_data to null, so that it will be destroyed.
                    block_data = null;

                    // Update the block's neighbors' surround counts.
                    MapView.instance.UpdateNeighboringSurroundCounts(blockX, blockZ, false);
                }

                // Determine when the next update should be, if there is a pending transition.
                DetermineNextUpdate();
            }
        }
        else 
        {
            // Determine and apply this object's current build appearance.
            cur_appearance = DetermineBuildAppearance(Time.time);
            ApplyBuildAppearance(cur_appearance);

            // Determine when the next update should be, if there is a pending transition.
            DetermineNextUpdate();
        }

        if ((timerText != null) && ((block_data == null) || (Time.time > block_data.completion_time))) {
            CleanUpTimerText();
        }
    }

    private void DetermineNextUpdate()
    {
        //Debug.Log("DetermineNextUpdate(): time: " + Time.time + ", transition_end_time: " + cur_appearance.transition_end_time);

        // If an appearance transition is in progress, record end time and next update time for transition.
        if (cur_appearance.transition_end_time > Time.time)
        {
            transition_end_appearance = DetermineBuildAppearance(cur_appearance.transition_end_time);
            next_update_time = 0f;
        }
        else
        {
            transition_end_appearance = null;

            if (block_data == null)
            {
                // This block's object has been deleted, and the transition has been shown. Clean up this BuildObject.
                CleanUp();
            }
            else
            {
                next_update_time = -1f;

                if (block_data.completion_time > Time.time) {
                    next_update_time = (next_update_time == -1f) ? block_data.completion_time : Mathf.Min(next_update_time, block_data.completion_time);
                }

                if (block_data.crumble_time > Time.time) {
                    next_update_time = (next_update_time == -1f) ? block_data.crumble_time : Mathf.Min(next_update_time, block_data.crumble_time);
                }

                if (block_data.invisible_time > Time.time) {
                    next_update_time = (next_update_time == -1f) ? block_data.invisible_time : Mathf.Min(next_update_time, block_data.invisible_time);
                }
            }
        }
    }

    private void DetermineAdjacentWalls()
    {
        // Determine whether there is a wall segment, with the same owner, adjacent to the east.
        BlockData east_block_data = MapView.instance.GetBlockData(blockX + 1, blockZ);
        wall_east = (east_block_data != null) && (east_block_data.objectID != -1) && ((east_block_data.crumble_time == -1) || (east_block_data.crumble_time > Time.time)) && (east_block_data.owner_nationID == block_data.owner_nationID) && (east_block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (BuildData.GetBuildData(east_block_data.objectID).type == BuildData.Type.WALL);

        // Determine whether there is a wall segment, with the same owner, adjacent to the south.
        BlockData south_block_data = MapView.instance.GetBlockData(blockX, blockZ + 1);
        wall_south = (south_block_data != null) && (south_block_data.objectID != -1) && ((south_block_data.crumble_time == -1) || (south_block_data.crumble_time > Time.time)) && (south_block_data.owner_nationID == block_data.owner_nationID) && (south_block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (BuildData.GetBuildData(south_block_data.objectID).type == BuildData.Type.WALL);

        // Determine whether there is a wall segment, with the same owner, adjacent to the west.
        BlockData west_block_data = MapView.instance.GetBlockData(blockX - 1, blockZ);
        wall_west = (west_block_data != null) && (west_block_data.objectID != -1) && ((west_block_data.crumble_time == -1) || (west_block_data.crumble_time > Time.time)) && (west_block_data.owner_nationID == block_data.owner_nationID) && (west_block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (BuildData.GetBuildData(west_block_data.objectID).type == BuildData.Type.WALL);

        // Determine whether there is a wall segment, with the same owner, adjacent to the north.
        BlockData north_block_data = MapView.instance.GetBlockData(blockX, blockZ - 1);
        wall_north = (north_block_data != null) && (north_block_data.objectID != -1) && ((north_block_data.crumble_time == -1) || (north_block_data.crumble_time > Time.time)) && (north_block_data.owner_nationID == block_data.owner_nationID) && (north_block_data.objectID < ObjectData.RESOURCE_OBJECT_BASE_ID) && (BuildData.GetBuildData(north_block_data.objectID).type == BuildData.Type.WALL);

        // Determine whether this wall segment is in the middle of a straight run of wall, with the north or west being the same type of wall segment.
        wall_run = (((wall_east == false) && (wall_west == false) && wall_north && wall_south && (north_block_data.objectID == block_data.objectID)) ||
                    ((wall_north == false) && (wall_south == false) && wall_east && wall_west && (west_block_data.objectID == block_data.objectID))); 

        // Determine whether the wall's post should be shown. It is not shown if this position is in the middle of a straight run of wall,
        // and if this wall type does not show posts in straight runs.
        wall_post = !(wall_run && (build_data.GetWallShowAllPosts() == false));
        //Debug.Log(blockX + "," + blockZ + ": wall_post: " + wall_post + ", wall_run: " + wall_run + ", show all: " + build_data.GetWallShowAllPosts());
    }

    private void UpdateAdjacents()
    {
        BlockData adj_block_data;
        
        // North
        adj_block_data = MapView.instance.GetBlockData(blockX, blockZ - 1);
        if ((adj_block_data != null) && (adj_block_data.build_object != null)) {
            adj_block_data.build_object.UpdateForChangeToAdjacent();
        }

        // South
        adj_block_data = MapView.instance.GetBlockData(blockX, blockZ + 1);
        if ((adj_block_data != null) && (adj_block_data.build_object != null)) {
            adj_block_data.build_object.UpdateForChangeToAdjacent();
        }

        // East
        adj_block_data = MapView.instance.GetBlockData(blockX + 1, blockZ);
        if ((adj_block_data != null) && (adj_block_data.build_object != null)) {
            adj_block_data.build_object.UpdateForChangeToAdjacent();
        }

        // West
        adj_block_data = MapView.instance.GetBlockData(blockX - 1, blockZ);
        if ((adj_block_data != null) && (adj_block_data.build_object != null)) {
            adj_block_data.build_object.UpdateForChangeToAdjacent();
        }
    }

    private void InstantiateObjects()
    {
        // Get the raid prefab (if there is one).
        raid_prefab = build_data.GetRaidPrefab();

        if (wall)
        {
            GameObject wall_length_prefab = build_data.GetWallLengthPrefab();

            if (wall_post)
            {
                // Instantiate the build object from its prefab, and set its position.
                object0 = Object.Instantiate(build_data.GetPrefab()) as GameObject;
                object0.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);

                if (build_data.original_name.Contains("Pyralisade")) {
                    object0.transform.localEulerAngles = new Vector3(0, Random.Range(0f, 360f), 0);
                }
            }

            if (wall_east)
            {
                object1 = Object.Instantiate(wall_length_prefab) as GameObject;
                object1.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);
            }

            if (wall_south)
            {
                object2 = Object.Instantiate(wall_length_prefab) as GameObject;
                object2.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);
                object2.transform.localEulerAngles = new Vector3(0, 90, 0);
            }

            //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " InstantiateObjects(), wall_post: " + wall_post + ", wall_east: " + wall_east + ", wall_south: " + wall_south + ", object0: " + object0 + ", object1: " + object1 + ", object2: " + object2);
        }
        else 
        {
            // Instantiate the build object from its prefab, and set its position.
            object0 = Object.Instantiate(build_data.GetPrefab()) as GameObject;
            object0.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);
        }

        if (object0 != null) build_data.PrepObject(object0);
        if (object1 != null) build_data.PrepObject(object1);
        if (object2 != null) build_data.PrepObject(object2);

        // Record that the objects have been instantiated.
        objects_instantiated = true;
    }

    private void DeleteObjects()
    {
        //if ((blockX == 15) && (blockZ == 34)) Debug.Log("Block " + blockX + "," + blockZ + " here4, object0: " + object0 + ", object1: " + object1 + ", object2: " + object2);

        if (object0)
        {
            ObjectData.DestroyAllMaterials(object0);
            Object.Destroy(object0);
            object0 = null;
        }

        if (object1)
        {
            ObjectData.DestroyAllMaterials(object1);
            Object.Destroy(object1);
            object1 = null;
        }

        if (object2)
        {
            ObjectData.DestroyAllMaterials(object2);
            Object.Destroy(object2);
            object2 = null;
        }
    }

    public void SetEmission(bool _enable)
    {
        if (object0 != null) {
            build_data.SetEmission(object0, _enable);
        }
    }

    public void ShowPortal(bool _enable)
    {
        if (build_data.original_name.Contains("Djinn Portal"))
        {
            if (object0 != null) {
                object0.GetComponent<DjinnPortal>().SetShowPortal(_enable);
            }
        }
    }

    public void ShowGenie(bool _enable)
    {
        if (build_data.original_name.Contains("Djinn Portal"))
        {
            if (object0 != null)
            {
                object0.GetComponent<DjinnPortal>().SetShowGenie(_enable);
            }
        }
    }

    private BuildAppearance DetermineBuildAppearance(float _time)
    {
        // Create a new BuildAppearance and fill in its values
        BuildAppearance appearance = new BuildAppearance();
        appearance.object_emission = emission_normal;
        appearance.construction_albedo = albedo_start_construction;
        appearance.cur_time = _time;
        appearance.transition_end_time = -1f;

        // If the build object's block is no longer represented on the client, do not attempt to determine appearance.
        if (block_data == null) {
            return appearance;
        }

        // Determine the normal and incomplete albedo values, given the state of the object.
        Color state_normal_albedo = albedo_normal;
        Color state_incomplete_albedo = albedo_incomplete;
        Color state_construction_albedo = albedo_normal;
        if (block_data.capture_time != -1)
        {
            if (block_data.owner_nationID == block_data.nationID)
            {
                if (_time >= (block_data.capture_time + TRANSITION_DURATION)) 
                {
                    state_normal_albedo = albedo_normal;
                    state_incomplete_albedo = albedo_incomplete;
                    state_construction_albedo = albedo_normal;
                }
                else 
                {
                    state_normal_albedo = Color.Lerp(albedo_captured, albedo_normal, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    state_incomplete_albedo = Color.Lerp(albedo_captured, albedo_incomplete, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    state_construction_albedo = Color.Lerp(albedo_captured, albedo_normal, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.capture_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.capture_time + TRANSITION_DURATION);

                    if (block_data.nationID == GameData.instance.nationID)
                    {
                        // Play sound
                        Sound.instance.PlayInWorld(Sound.instance.capture, MapView.instance.GetBlockCenterWorldPos(blockX, blockZ));
                    }
                }
                //Debug.Log("Here1 time: " + _time + ", capture_time: " + block_data.capture_time + ", state_normal_albedo.a: " + state_normal_albedo.a);  
            }
            else 
            {
                // Determine this BuildObject's albedo for when captured. Shard objects use the normal albedo when captured.
                Color build_albedo_catured = (build_data.type == BuildData.Type.SHARD) ? albedo_normal : albedo_captured;

                if (_time >= (block_data.capture_time + TRANSITION_DURATION)) 
                {
                    state_normal_albedo = build_albedo_catured;
                    state_incomplete_albedo = build_albedo_catured;
                    state_construction_albedo = build_albedo_catured;
                }
                else 
                {
                    state_normal_albedo = Color.Lerp(albedo_normal, build_albedo_catured, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    state_incomplete_albedo = Color.Lerp(albedo_incomplete, build_albedo_catured, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    state_construction_albedo = Color.Lerp(albedo_normal, build_albedo_catured, (_time - block_data.capture_time) / TRANSITION_DURATION);
                    appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.capture_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.capture_time + TRANSITION_DURATION);

                    if (block_data.nationID == GameData.instance.nationID)
                    {
                        // Play sound
                        Sound.instance.PlayInWorld(Sound.instance.capture, MapView.instance.GetBlockCenterWorldPos(blockX, blockZ));
                    }
                }
                //Debug.Log("Here2 time: " + _time + ", capture_time: " + block_data.capture_time + ", state_normal_albedo.a: " + state_normal_albedo.a);  
            }
        }

        // If the object is not under construction or transitioning out, the construction object should not show at all.
        if ((block_data.completion_time == -1) || (_time > (block_data.completion_time + TRANSITION_DURATION))) {
            state_construction_albedo = albedo_start_construction;
        }

        // Determine the object albedo and emission values, and construction albedo value, based on the current 
        // phase in the object's life cycle, as well as on the normal and incomplete albedo values determined above.
        if ((block_data.crumble_time != -1) && (_time >= (block_data.crumble_time + TRANSITION_DURATION))) 
        {
            //Debug.Log("here1");
            appearance.object_albedo = albedo_end_crumble;
            appearance.construction_albedo = albedo_end_crumble;
            //Debug.Log("Here2 time: " + _time + ", crumble_time: " + block_data.crumble_time + ", object_albedo.a: " + appearance.object_albedo.a);  
        }
        else if ((block_data.crumble_time != -1) && (_time >= block_data.crumble_time)) 
        {
            //Debug.Log("here2");
            appearance.object_albedo = Color.Lerp(state_normal_albedo, albedo_end_crumble, (_time - block_data.crumble_time) / TRANSITION_DURATION);
            appearance.construction_albedo = Color.Lerp(state_construction_albedo, albedo_end_crumble, (_time - block_data.crumble_time) / TRANSITION_DURATION);
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.crumble_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.crumble_time + TRANSITION_DURATION);
            //Debug.Log("Here1 time: " + _time + ", crumble_time: " + block_data.crumble_time + ", object_albedo.a: " + appearance.object_albedo.a);  
        }
        else if ((salvage_time != -1) && (_time >= (salvage_time + TRANSITION_DURATION))) 
        {
            //Debug.Log("here1");
            appearance.object_albedo = albedo_end_salvage;
            appearance.object_emission = emission_end_salvage;
            appearance.construction_albedo = albedo_end_salvage;
        }
        else if ((salvage_time != -1) && (_time >= salvage_time)) 
        {
            //Debug.Log("here2");
            appearance.object_albedo = Color.Lerp(state_normal_albedo, albedo_end_salvage, (_time - salvage_time) / TRANSITION_DURATION);
            appearance.object_emission = Color.Lerp(emission_normal, emission_end_salvage, (_time - salvage_time) / TRANSITION_DURATION);
            appearance.construction_albedo = Color.Lerp(state_construction_albedo, albedo_end_salvage, (_time - salvage_time) / TRANSITION_DURATION);
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (salvage_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, salvage_time + TRANSITION_DURATION);

            // Play sound
            Sound.instance.PlayInWorld(Sound.instance.salvage, MapView.instance.GetBlockCenterWorldPos(blockX, blockZ));
        }
        else if ((block_data.completion_time == -1) || (_time >= (block_data.completion_time + TRANSITION_DURATION))) 
        {
            //Debug.Log("here3");
            appearance.object_albedo = state_normal_albedo;
        }
        else if (_time >= block_data.completion_time) 
        {
            if (block_data.completion_time > block_data.creation_time)
            {
                // Object goes through a period of construction.
                //Debug.Log("here4a");
                appearance.object_albedo = Color.Lerp(state_incomplete_albedo, state_normal_albedo, (_time - block_data.completion_time) / TRANSITION_DURATION);
                appearance.construction_albedo = Color.Lerp(state_construction_albedo, albedo_start_construction, (_time - block_data.completion_time) / TRANSITION_DURATION);
                appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.completion_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.completion_time + TRANSITION_DURATION);

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.build_complete, MapView.instance.GetBlockCenterWorldPos(blockX, blockZ));
            }
            else
            {
                // Object does not go through a period of construction.
                //Debug.Log("here4b");
                appearance.object_albedo = Color.Lerp(albedo_start_construction, state_normal_albedo, (_time - block_data.completion_time) / TRANSITION_DURATION);
                appearance.object_emission = Color.Lerp(emission_start_construction, emission_normal, (_time - block_data.completion_time) / TRANSITION_DURATION);
                appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.completion_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.completion_time + TRANSITION_DURATION);
            }
        }
        else if ((block_data.creation_time == -1) || (_time >= (block_data.creation_time + TRANSITION_DURATION))) 
        {
            //Debug.Log("here5");
            appearance.object_albedo = state_incomplete_albedo;
            appearance.construction_albedo = state_construction_albedo;
        }
        else 
        {
            //Debug.Log("here6");
            appearance.object_albedo = Color.Lerp(albedo_start_construction, state_incomplete_albedo, (_time - block_data.creation_time) / TRANSITION_DURATION);
            appearance.object_emission = Color.Lerp(emission_start_construction, emission_normal, (_time - block_data.creation_time) / TRANSITION_DURATION);
            appearance.construction_albedo = Color.Lerp(albedo_start_construction, state_construction_albedo, (_time - block_data.creation_time) / TRANSITION_DURATION);
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (block_data.creation_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, block_data.creation_time + TRANSITION_DURATION);
        }

        // Inert
        if ((inert_end_time != -1) && (_time >= block_data.build_object.inert_end_time) && (_time < (inert_end_time + TRANSITION_DURATION))) 
        {
            appearance.object_emission = Color.Lerp(new Color(1f,0f,0f,1f), emission_normal, (_time - inert_end_time) / TRANSITION_DURATION);
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (inert_end_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, inert_end_time + TRANSITION_DURATION);
        }
        else if ((inert_start_time != -1) && (_time >= (inert_start_time + TRANSITION_DURATION))) 
        {
            appearance.object_emission = new Color(1f, 0f, 0f, 1f);
        }
        else if ((inert_start_time != -1) && (_time >= inert_start_time))
        {
            appearance.object_emission = Color.Lerp(emission_normal, new Color(1f,0f,0f,1f), (_time - inert_start_time) / TRANSITION_DURATION);
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (inert_start_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, inert_start_time + TRANSITION_DURATION);
        }

        // Invisible
        if ((invisible_end_time != -1) && (_time >= invisible_end_time) && (_time < (invisible_end_time + TRANSITION_DURATION))) 
        {
            appearance.object_albedo = new Color(appearance.object_albedo.r, appearance.object_albedo.g, appearance.object_albedo.b, Mathf.Min(appearance.object_albedo.a, Mathf.Lerp(0f, appearance.object_albedo.a, (_time - invisible_end_time) / TRANSITION_DURATION)));
            appearance.construction_albedo = new Color(appearance.construction_albedo.r, appearance.construction_albedo.g, appearance.construction_albedo.b, Mathf.Min(appearance.object_albedo.a, Mathf.Lerp(0f, appearance.construction_albedo.a, (_time - invisible_end_time) / TRANSITION_DURATION)));
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (invisible_end_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, invisible_end_time + TRANSITION_DURATION);

            // Play sound
            Sound.instance.PlayInWorld(Sound.instance.appear, MapView.instance.GetBlockCenterWorldPos(blockX, blockZ));
        }
        else if ((invisible_start_time != -1) && (_time >= (invisible_start_time + TRANSITION_DURATION))) 
        {
            appearance.object_albedo = new Color(appearance.object_albedo.r, appearance.object_albedo.g, appearance.object_albedo.b, 0f);
            appearance.construction_albedo = new Color(appearance.construction_albedo.r, appearance.construction_albedo.g, appearance.construction_albedo.b, 0f);
        }
        else if ((invisible_start_time != -1) && (_time >= invisible_start_time))
        {
            appearance.object_albedo = new Color(appearance.object_albedo.r, appearance.object_albedo.g, appearance.object_albedo.b, Mathf.Min(appearance.object_albedo.a, Mathf.Lerp(appearance.object_albedo.a, 0f, (_time - invisible_start_time) / TRANSITION_DURATION)));
            appearance.construction_albedo = new Color(appearance.construction_albedo.r, appearance.construction_albedo.g, appearance.construction_albedo.b, Mathf.Min(appearance.construction_albedo.a, Mathf.Lerp(appearance.construction_albedo.a, 0f, (_time - invisible_start_time) / TRANSITION_DURATION)));
            appearance.transition_end_time = (appearance.transition_end_time == -1) ? (invisible_start_time + TRANSITION_DURATION) : Mathf.Min(appearance.transition_end_time, invisible_start_time + TRANSITION_DURATION);
        }

        // Determine whether this object is currently invisible.
        bool invisible = ((block_data.nationID == block_data.owner_nationID) && (block_data.nationID != GameData.instance.nationID) && (invisible_start_time != -1) && (_time >= invisible_start_time));

        // Set the object's turret motion's mute setting (if it has a TurretMotion component), according to whether it is invisible.
        TurretMotion turret_motion = (object0 == null) ? null : object0.GetComponent<TurretMotion>();
        if (turret_motion != null) turret_motion.mute = invisible;

        // Display the storage meter if appropriate
        if (RequiresStorageMeter() && (storageMeter == null) && (_time >= block_data.completion_time)) {
            InitStorageMeter();
        }

        //Debug.Log("DetermineBuildAppearance(); time: " + _time + ", salvage_time:" + salvage_time + ", completion_time: " + block_data.completion_time + ", creation_time: " + block_data.creation_time + ", inert_start_time: " + inert_start_time + ", inert_end_time: " + inert_end_time + ", transition_end_time" + appearance.transition_end_time);

        return appearance;
    }

    private void ApplyBuildAppearance(BuildAppearance _appearance)
    {
        if (_appearance.construction_albedo.a > 0f)
        {
            if (construction == null)
            {
                // Instantiate the construction object
                construction = Object.Instantiate(BuildPrefabs.instance.construction) as GameObject;
                construction.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);

                // Randomly rotate the construction object, according to hash based on block coordinates.
                construction.transform.localEulerAngles = new Vector3(0f, (int)(GameData.instance.xxhash.GetHashFloat(blockX, blockZ) * 4f) * 90, 0f);
            }

            // Apply the construction_albedo to the construction object.
            ApplyAppearanceRecursively(construction, _appearance.construction_albedo, emission_normal);
        }
        else 
        {
            if (construction != null)
            {
                // Destroy the construction object
                ObjectData.DestroyAllMaterials(construction);
                Object.Destroy(construction);
                construction = null;
            }
        }
    
        // Apply the albedo and emission to each object.
        if (object0 != null) ApplyAppearanceRecursively(object0, _appearance.object_albedo, _appearance.object_emission);
        if (object1 != null) ApplyAppearanceRecursively(object1, _appearance.object_albedo, _appearance.object_emission);
        if (object2 != null) ApplyAppearanceRecursively(object2, _appearance.object_albedo, _appearance.object_emission);
    }

    private void ApplyAppearanceRecursively(GameObject _gameObject, Color _albedo, Color _emission)
    {
        Renderer rend;
        foreach (Transform child in _gameObject.transform)
        {
            rend = child.gameObject.GetComponent<Renderer>();
            if (rend != null) 
            {
                //Debug.Log("ApplyAppearanceRecursively() top: " + _gameObject.name + ", cur:" + child.gameObject.name + ", emission: " + _emission);
                //if (_gameObject.name.Equals("Plasma Screen II")) {
                //    Debug.Log("ApplyAppearanceRecursively() top: " + _gameObject.name + ", cur:" + child.gameObject.name + ", _emission: " + _emission.r + "," + _emission.g + "," + _emission.b + "," + _emission.a);
                //}

                foreach (Material mat in rend.materials)
                {
                    // The tricky bit below is to allow a material to be set to a given non-zero emission value when needed, but when set to 0 emission,
                    // it will fall back to employing the material's original _EmissionMap if it had one. If a non-zero emission value is given, then the
                    // emission map is temporarily stored as the material's _DetailNormalMap. Once the material's given emission is set back to 0, the
                    // emission map will be restored, and the _DetailNormalMap will be set back to null. (Note that this means no material should ever make actual
                    // use of the _DetailNormalMap.)
                    if ((_emission.a > 0) && (mat.HasProperty("_EmissionMap")) && (mat.GetTexture("_EmissionMap") != null))
                    {
                        mat.SetTexture("_DetailNormalMap", mat.GetTexture("_EmissionMap"));
                        mat.SetTexture("_EmissionMap", null);
                    }
                    else if ((_emission.a == 0) && (mat.HasProperty("_DetailNormalMap")) && (mat.GetTexture("_DetailNormalMap") != null))
                    {
                        mat.SetTexture("_EmissionMap", mat.GetTexture("_DetailNormalMap"));
                        mat.SetTexture("_DetailNormalMap", null);
                    }

                    mat.SetColor("_Color", _albedo);

                    // If the material has an emission property, set it to the given emission. If not, try to simulate the effect by changing the albedo color.
                    if (mat.HasProperty("_EmissionColor")) {
                        mat.SetColor("_EmissionColor", ((_emission.a > 0) || (mat.HasProperty("_EmissionMap") == false) || (mat.GetTexture("_EmissionMap") == null)) ? _emission : new Color (1,1,1,1));
                    } else if (_emission.a > 0) {
                        Color color = new Color(_emission.r, _emission.g, _emission.b, 1f - (_emission.r * 0.3f + _emission.g * 0.59f + _emission.b * 0.11f));
                        mat.SetColor("_Color", color);
                    }
                }

                // If the material has an _EmissionMap, then set the emission value to 1 so the map will be used.
                if ((_emission.a == 0f) && (rend.material.HasProperty("_EmissionMap")) && (rend.material.GetTexture("_EmissionMap") != null)) {
                    rend.material.SetColor("_EmissionColor", new Color(1, 1, 1, 1));
                }

                // if the _emission value is non-zero, or the material has an emission map, turn on emission for the material. Otehrwise turn it off.
                if ((_emission.a > 0f) || ((rend.material.HasProperty("_EmissionMap")) && (rend.material.GetTexture("_EmissionMap") != null))) {
                    rend.material.EnableKeyword ("_EMISSION");
                } else {
                    rend.material.DisableKeyword ("_EMISSION");
                }
            }

            ApplyAppearanceRecursively(child.gameObject, _albedo, _emission);
        }
    }

    public void Aim(int _target_x, int _target_z, float _duration)
    {
        // Do nothing if the object has not been instantiated.
        if (object0 == null) {
            return;
        }

        if ((build_data.type == BuildData.Type.DIRECTED_MULTIPLE) || (build_data.type == BuildData.Type.SPLASH) || (build_data.type == BuildData.Type.COUNTER_ATTACK) ||
            ((build_data.type == BuildData.Type.RECAPTURE) && (build_data.original_name.Contains("Ecto Ray"))))
        {
            object0.GetComponent<TurretMotion>().SetTargetPosition(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z), _duration);
        }
        else if ((build_data.type == BuildData.Type.RECAPTURE) && (build_data.original_name.Contains("Djinn Portal")))
        {
            object0.GetComponent<DjinnPortal>().Aim(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z), _duration);
        }
    }

    public void Fire(int _target_x, int _target_z, float _duration)
    {
        RocketGun rocket_gun;
        LaserGun laser_gun;

        if (object0 == null) {
            return; // This object has been removed frm the map since the coroutine that is causing it to fire was initiated. 
        }

        if (build_data.type == BuildData.Type.DIRECTED_MULTIPLE)
        {
            // For each of the current turret's barrels...
            foreach(Transform barrel in object0.transform.GetChild(0).transform.GetChild(0).transform) 
            {
                // Fire each gun child of the barrel object, at slightly different targets.
                foreach(Transform child in barrel) 
                {
                    rocket_gun = child.gameObject.GetComponent<RocketGun>();
                    if (rocket_gun != null) {
                        // Add some random deviation to the target position to appear more natural.
                        rocket_gun.Fire(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z) + new Vector3(Random.Range(-3f, 3f), 0, Random.Range(-3f, 3f)));
                    }
                }
            }
        }
        else if (build_data.type == BuildData.Type.SPLASH)
        {
            float fire_delay = 0f;

            if (build_data.original_name.Contains("Pestilence Launcher"))
            {
                // Swing and then reset the Pestilence Launcher's arm.
                Transform arm_object = object0.transform.GetChild(0).transform.GetChild(0).transform.GetChild(0).transform;
                StartCoroutine(Rotation_Coroutine(arm_object, 0f, 0.2f, new Vector3(0,26,0), new Vector3(0,-35,0)));
                StartCoroutine(Rotation_Coroutine(arm_object, 0.2f, 1f, new Vector3(0,-35,0), new Vector3(0,26,0)));
                fire_delay = .1f; // Slight delay before firing, to allow catapult arm to swing.
            }

            // For each of the current turret's barrels...
            Transform barrels_parent = (build_data.original_name.Contains("Nanobot Swarm Base")) ? object0.transform.GetChild(0).transform.GetChild(0).transform.GetChild(0).transform : object0.transform.GetChild(0).transform.GetChild(0).transform;
            foreach(Transform barrel in barrels_parent) 
            {
                // Fire each gun child of the barrel object, at slightly different targets.
                foreach(Transform child in barrel) 
                {
                    rocket_gun = child.gameObject.GetComponent<RocketGun>();
                    if (rocket_gun != null) {
                        // Hit direct center of block, so that splash effect radius matches actual effect radius.
                        rocket_gun.FireAfterDelay(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z), fire_delay);
                    }
                }
            }
        }
        else if (build_data.type == BuildData.Type.AREA_EFFECT)
        {
            Vector3 start_pos = object0.transform.GetChild(0).transform.position;
            Vector3 target_pos = MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z);
            Vector3 direction = target_pos - start_pos;

            if (build_data.original_name.Contains("Telekinetic Projector"))
            {
                GameObject fx = (GameObject)GameObject.Instantiate(BuildPrefabs.instance.iceWave, start_pos, BuildPrefabs.instance.iceWave.transform.rotation);
                fx.transform.forward = direction;
                fx.transform.localScale = new Vector3(5f, 5f, 5f);

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.teleproj, object0.transform.position);
            }
            else if (build_data.original_name.Contains("Pyroclasm"))
            {
                GameObject fx = (GameObject)GameObject.Instantiate(BuildPrefabs.instance.fireball, start_pos, BuildPrefabs.instance.fireball.transform.rotation);
                fx.transform.forward = direction;
                fx.transform.localScale = new Vector3(5f, 5f, 5f);

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.fireball, object0.transform.position);
            }
            else if (build_data.original_name.Contains("Keraunocon"))
            {
                TeslaGun tesla_gun = object0.transform.GetChild(0).gameObject.GetComponent<TeslaGun>();
                if (tesla_gun != null) 
                {
                    tesla_gun.life = _duration;
                    tesla_gun.Fire(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z) + new Vector3(Random.Range(-3f, 3f), 0, Random.Range(-3f, 3f)));
                }

                // Play sound
                Sound.instance.PlayInWorldForDuration(Sound.instance.electricity, object0.transform.position, _duration);
            }
        }
        else if (build_data.type == BuildData.Type.RECAPTURE)
        {
            if (build_data.original_name.Contains("Ecto Ray"))
            {
                // For each of the current turret's barrels...
                foreach(Transform barrel in object0.transform.GetChild(0).transform.GetChild(0).transform)
                {
                    // Fire each gun child of the barrel object.
                    foreach(Transform child in barrel) 
                    {
                        laser_gun = child.gameObject.GetComponent<LaserGun>();
                        if (laser_gun != null) {
                            laser_gun.Fire();
                        }
                    }
                }

                // Play sound
                Sound.instance.PlayInWorld(Sound.instance.slime, object0.transform.position);
            }
            else if (build_data.original_name.Contains("Djinn Portal"))
            {
                object0.GetComponent<DjinnPortal>().Fire(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z));
            }
        }
        else if (build_data.type == BuildData.Type.TOWER_BUSTER)
        {
            rocket_gun = object0.transform.GetChild(0).GetComponent<RocketGun>();
            if (rocket_gun != null) {
                rocket_gun.Fire(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z));
            }
        }
        else if (build_data.type == BuildData.Type.AIR_DROP)
        {
            // Determine the position for the source of the beam effect.
            Vector3 source_position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);

            // Height of source
            if (build_data.original_name.Contains("Brainsweeper III")) {
                source_position.y += 12f; 
            } else if (build_data.original_name.Contains("Brainsweeper II")) {
                source_position.y += 10f;
            } else if (build_data.original_name.Contains("Brainsweeper")) {
                source_position.y += 8f;
            }

            // Instantiate the beam effect and set its positon and rotation appropriately.
            GameObject fx_object = Object.Instantiate(BuildPrefabs.instance.brainsweeperFX) as GameObject;
            fx_object.transform.position = source_position;
            fx_object.transform.localRotation = Quaternion.LookRotation(MapView.instance.GetBlockCenterWorldPos(_target_x, _target_z) - source_position);

            // Tell the particle effect when to end.
            fx_object.GetComponent<ParticleFXManager>().SetEndTime(Time.time + 5f);

            // Play sound
            Sound.instance.PlayInWorld(Sound.instance.brainsweeper, source_position);

            if (build_data.original_name.Contains("Brainsweeper III"))
            {
                // Play the activation animation.
                Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                animation["bs3_activate"].speed = 0.5f;
                StartCoroutine(Animation_Coroutine(animation, "bs3_activate", 0f));
            }
        }
    }

    public void Activate()
    {
        if ((block_data != null) && (!invisible) && (object0 != null))
        {
            if (build_data.type == BuildData.Type.AREA_FORTIFICATION)
            {
                if (build_data.original_name.Contains("Tree Summoner"))
                {
                    // Play the summoning animation, twice.
                    Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                    if (build_data.original_name.Contains("III"))
                    {
                        animation["TreeSummon3"].speed = 0.5f;
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon3", 0f));
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon3", 1.75f));
                    }
                    else if (build_data.original_name.Contains("II"))
                    {
                        animation["TreeSummon2"].speed = 0.5f;
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon2", 0f));
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon2", 1.75f));
                    }
                    else
                    {
                        animation["TreeSummon1"].speed = 0.5f;
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon1", 0f));
                        StartCoroutine(Animation_Coroutine(animation, "TreeSummon1", 1.75f));
                    }

                    // Play the thumping sounds
                    Sound.instance.PlayInWorldAfterDelay(1.0f, Sound.instance.explosion4, object0.transform.position, 1.4f);
                    Sound.instance.PlayInWorldAfterDelay(2.7f, Sound.instance.explosion4, object0.transform.position, 1.4f);
                }
            }
            else if (build_data.type == BuildData.Type.SPECIFIC_LASTING_WIPE)
            {
                if (build_data.original_name.Contains("Hypnotic Inducer"))
                {
                    object0.transform.GetChild(0).GetChild(0).gameObject.GetComponent<HypnoSpin>().Spin(9f);
                }
            }
            else if (build_data.type == BuildData.Type.GENERAL_LASTING_WIPE)
            {
                if (build_data.original_name.Contains("Supervirus Contagion"))
                {
                    // Play the activation animation.
                    Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                    animation["supervirus active"].speed = 0.5f;
                    StartCoroutine(Animation_Coroutine(animation, "supervirus active", 0f));
                }
                else if (build_data.original_name.Equals("Toxic Chemical Dump"))
                {
                    // Play the activation animation.
                    Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                    animation["toxchemdump1 active"].speed = 0.2f;
                    StartCoroutine(Animation_Coroutine(animation, "toxchemdump1 active", 0f));
                }
                else if (build_data.original_name.Equals("Toxic Chemical Dump II"))
                {
                    // Play the activation animation.
                    Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                    animation["toxchemdump2 active"].speed = 0.2f;
                    StartCoroutine(Animation_Coroutine(animation, "toxchemdump2 active", 0f));
                }
                else if (build_data.original_name.Equals("Toxic Chemical Dump III"))
                {
                    // Play the activation animation.
                    Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                    animation["toxchemdump3 active"].speed = 0.2f;
                    StartCoroutine(Animation_Coroutine(animation, "toxchemdump3 active", 0f));
                }
            }
        }

        // Record time when this object was previously activated
        prev_activate_time = Time.time;
    }

    public bool IsTargeting()
    {
        if (object0 == null) {
            return false;
        }

        if ((build_data.type == BuildData.Type.DIRECTED_MULTIPLE) || (build_data.type == BuildData.Type.SPLASH) ||
            ((build_data.type == BuildData.Type.RECAPTURE) && (build_data.original_name.Contains("Ecto Ray"))))
        {
            if (object0.GetComponent<TurretMotion>() == null) Debug.Log("Object " + object0.name + " has no TurretMotion.");
            return object0.GetComponent<TurretMotion>().targeting;
        }

        return false;
    }
    
    public IEnumerator Animation_Coroutine(Animation _animation, string _anim_name, float _delay)
    {
        yield return new WaitForSeconds(_delay);
        _animation.Play(_anim_name);
    }

    public IEnumerator Rotation_Coroutine(Transform _transform, float _delay, float _duration, Vector3 _start_rotation, Vector3 _end_rotation)
    {
        // Wait for the given _delay
        if (_delay > 0f) {
            yield return new WaitForSeconds(_delay);
        }

        float start_time = Time.time;
        float degree;
        Vector3 new_rot;
        for (;;)
        {
            // Determine the current degree based on how much time has elapsed.
            degree = (Time.time - start_time) / _duration;
            degree = Mathf.Min(degree, 1f);
            // Determine and set the new rotation.
            new_rot = Vector3.Lerp(_start_rotation, _end_rotation, degree);
            _transform.localEulerAngles = new_rot;

            // Exit loop if we've finished.
            if (degree == 1f) {
                break;
            }

            // Wait until thenext frame.
            yield return null;
        }
    }

    public IEnumerator LookAlive()
    {
        for (; ;)
        {
            yield return new WaitForSeconds(Random.Range(MIN_LOOK_ALIVE_PERIOD, MAX_LOOK_ALIVE_PERIOD));

            if ((block_data != null) && (IsTargeting() == false) && (!inert) && (block_data.nationID == block_data.owner_nationID) && (block_data.completion_time < Time.time))
            {
                SetRandomTargetBlock(blockX, blockZ, block_data);
                Aim(block_data.target_x, block_data.target_z, Random.Range(1f, 3f));
            }
        }
    }

    public IEnumerator Idle()
    {
        yield return new WaitForSeconds(Random.Range(0.5f, 5));

        for (; ;)
        {
            if ((block_data != null) && (IsTargeting() == false) && (!inert) && (!invisible) && ((prev_activate_time == -1) || (prev_activate_time < (Time.time - 8))) && (block_data.nationID == block_data.owner_nationID) && (block_data.completion_time < Time.time))
            {
                if (object0 != null)
                {
                    if ((build_data.type == BuildData.Type.AIR_DROP) && (build_data.original_name.Contains("Brainsweeper III")))
                    {
                        // Play the idle animation.
                        Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                        animation["bs3_idle"].speed = 0.5f;
                        StartCoroutine(Animation_Coroutine(animation, "bs3_idle", 0f));
                    }
                    else if ((build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) && (build_data.original_name.Contains("Supervirus Contagion")))
                    {
                        // Play the idle animation.
                        Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                        animation["supervirus idle"].speed = 0.5f;
                        StartCoroutine(Animation_Coroutine(animation, "supervirus idle", 0f));
                    }
                    else if ((build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) && (build_data.original_name.Equals("Toxic Chemical Dump")))
                    {
                        // Play the idle animation.
                        Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                        animation["toxchemdump1 idle"].speed = 0.3f;
                        StartCoroutine(Animation_Coroutine(animation, "toxchemdump1 idle", 0f));
                    }
                    else if ((build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) && (build_data.original_name.Equals("Toxic Chemical Dump II")))
                    {
                        // Play the idle animation.
                        Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                        animation["toxchemdump2 idle"].speed = 0.3f;
                        StartCoroutine(Animation_Coroutine(animation, "toxchemdump2 idle", 0f));
                    }
                    else if ((build_data.type == BuildData.Type.GENERAL_LASTING_WIPE) && (build_data.original_name.Equals("Toxic Chemical Dump III")))
                    {
                        // Play the idle animation.
                        Animation animation = object0.transform.GetChild(0).gameObject.GetComponent<Animation>();
                        animation["toxchemdump3 idle"].speed = 0.3f;
                        StartCoroutine(Animation_Coroutine(animation, "toxchemdump3 idle", 0f));
                    }
                }
            }

            yield return new WaitForSeconds(IDLE_PERIOD);
        }
    }

    public void SetRandomTargetBlock(int _blockX, int _blockZ, BlockData _block_data)
    {
        _block_data.target_x = _blockX + Random.Range(-5, 5);
        _block_data.target_z = _blockZ + Random.Range(-5, 5);
    }

    public void DisplayRaid(int _start_x, int _start_z, int _end_x, int _end_z, float _duration)
    {
        // If this object has no raid prefab, we can't display a raid.
        if (raid_prefab == null) {
            return;
        }

        // Play sound.
        AudioClip audio_clip = build_data.GetRaidAudioClip();
        if (audio_clip != null) {
            Sound.instance.PlayInWorld(audio_clip, object0.transform.position);
        }

        // Start coroutine to display raid.
        StartCoroutine(DisplayRaid_Coroutine(_start_x, _start_z, _end_x, _end_z, _duration));
    }
    
    public IEnumerator DisplayRaid_Coroutine(int _start_x, int _start_z, int _end_x, int _end_z, float _duration)
    {
        // Determine start and end positions for the raid object.
        Vector3 start_pos = MapView.instance.GetBlockCenterWorldPos(_start_x, _start_z);
        Vector3 end_pos = MapView.instance.GetBlockCenterWorldPos(_end_x, _end_z);

        // Instantiate the raid object.
        raid_object = Object.Instantiate(raid_prefab) as GameObject;

        // Set the raid object's rotation, initial position and alpha.
        raid_object.transform.position = start_pos;
        raid_object.transform.localRotation = Quaternion.LookRotation(end_pos - start_pos);
        ApplyAlphaRecursively(raid_object, 0f);

        float start_time = Time.time;
        float end_time = start_time + _duration;
        float progress, cur_time;
        
        // Move the raid object, and fade it in and out over time.
        while ((cur_time = Time.time) < end_time)
        {
            progress = (cur_time - start_time) / _duration;
            raid_object.transform.position = Vector3.Lerp(start_pos, end_pos, progress);

            // Fade the raid object in and out.
            if (progress <= 0.1f) {
                ApplyAlphaRecursively(raid_object, progress / 0.1f);
            } else if (progress >= 0.9f) {
                ApplyAlphaRecursively(raid_object, 1f - ((progress - 0.9f) / 0.1f));
            }

            yield return null;
        }

        // Destroy the raid object.
        ObjectData.DestroyAllMaterials(raid_object);
        Object.Destroy(raid_object);
        raid_object = null;
    }

    private void ApplyAlphaRecursively(GameObject _gameObject, float _alpha)
    {
        Color albedo = new Color(1f, 1f, 1f, _alpha);

        //Debug.Log("ApplyAlphaRecursively(" + _alpha + ") top: " + _gameObject.name);

        Renderer rend;
        foreach (Transform child in _gameObject.transform)
        {
            rend = child.gameObject.GetComponent<Renderer>();
            if (rend != null) 
            {
                //Debug.Log("ApplyAlphaRecursively(" + _alpha + ") top: " + _gameObject.name + ", cur:" + child.gameObject.name);
                rend.material.SetColor("_Color", albedo);

                if (_alpha > 0f) {
                    rend.material.EnableKeyword ("_EMISSION");
                } else {
                    rend.material.DisableKeyword ("_EMISSION");
                }
            }

            ApplyAlphaRecursively(child.gameObject, _alpha);
        }
    }

    private class BuildAppearance
    {
        public Color object_albedo = new Color();
        public Color object_emission = new Color();
        public Color construction_albedo = new Color();
        public float cur_time = -1f;
        public float transition_end_time = -1f;

        public static BuildAppearance Lerp(BuildAppearance _val0, BuildAppearance _val1, float _time)
        {
            BuildAppearance result = new BuildAppearance();
 
            result.cur_time = _time;
            result.transition_end_time = _val0.transition_end_time;
            float t = (_time - _val0.cur_time) / (_val1.cur_time - _val0.cur_time);
            result.object_albedo = Color.Lerp(_val0.object_albedo, _val1.object_albedo, t);
            result.object_emission = Color.Lerp(_val0.object_emission, _val1.object_emission, t);
            result.construction_albedo = Color.Lerp(_val0.construction_albedo, _val1.construction_albedo, t);

            //Debug.Log("BuildAppearance.Lerp() _val0.cur_time: " + _val0.cur_time + ", _val1.cur_time: " + _val1.cur_time + ", _time: " + _time +", t: " + t + ", result.object_emission.r: " + result.object_emission.r + ", _val0.object_emission: " + _val0.object_emission.r + ", _val1.object_emission: " + _val1.object_emission.r);

            return result;
        }
    }
}
