using UnityEngine;
using System;
using System.Collections;


public class BlockProcess
{
    public static ArrayList block_process_list = new ArrayList();

    // Types of process
    public const int PROCESS_EVACUATE = 0;
    public const int PROCESS_OCCUPY = 1;
    public const int PROCESS_BATTLE = 2;

    private const float HIT_POINTS_FADE_DURATION = 0.5f;
    private const float EFFECT_HEAD_START_DURATION = 0.4f;
    private const float EFFECT_DURATION = 3f;

    private const float SOUND_TIME_OFFSET_EVACUATE = -0.2f;
    private const float SOUND_TIME_OFFSET_OCCUPY = -0.4f;

    private Color evacuate_effect_color = new Color(1f, 0.56f, 0f);

    int x, z;
    int nationID, prevNationID;
    float hit_points_start, hit_points_end, hit_points_full, hit_points_rate, hit_points_difference;
    float delay;
    int process_type, battle_flags;
    GameObject hit_points_display, effect;
    float effect_appear_time, change_state_time, play_sound_time, finished_time;
    bool state_changed, sound_played;
    Vector3 effect_position;

    public static void ActivateNew(int _x, int _z, int _process_type, int _nationID, float _delay, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _hit_points_rate, int _battle_flags)
    {
        if (MapView.instance.BlockOutsideViewData(_x, _z))
        {
            Debug.Log("ERROR: BlockProcess Activate() called for block " + _x + "," + _z + "; outside of view data (" + MapView.instance.viewDataBlockX0 + "," + MapView.instance.viewDataBlockZ0 + " to " + MapView.instance.viewDataBlockX1 + "," + MapView.instance.viewDataBlockZ1 + ")");
            return;
        }

        // Get the first free BlockProcess object, and increment the count of BlockProcess objects in use.
        BlockProcess block_process = MemManager.instance.GetBlockProcessObject();
        block_process_list.Add(block_process);

        block_process.Activate(_x, _z, _process_type, _nationID, _delay, _hit_points_start, _hit_points_end, _hit_points_full, _hit_points_rate, _battle_flags);
    }

    private static void Deactivate(BlockProcess _block_process)
    {
        //if (_block_process.x == 20) Debug.Log("BlockProcess.Deactivate() for block " + _block_process.x + "," + _block_process.z + ", new nationID: " + _block_process.nationID);

        // Free this BlockProcess
        block_process_list.Remove(_block_process);
        MemManager.instance.ReleaseBlockProcessObject(_block_process);
    }

    public static void UpdateAllActivity()
    {
        BlockProcess block_process;

        // Iterate through all active BlockProcess objects...
        for (int i = 0; i < block_process_list.Count; i++)
        {
            // Get the current active BlockProcess
            block_process = (BlockProcess)(block_process_list[i]);

            // Update the current active BlockProcess and determine whether it's finished.
            if (block_process.UpdateActivity() == true)
            {
                // This BlockProcess is finished. Deactivate it.
                Deactivate(block_process);
                i--;
            }
        }
    }

    public static void UpdateAllForMapAreaChange()
    {
        BlockProcess block_process;

        // Iterate through all active BlockProcess objects...
        for (int i = 0; i < block_process_list.Count; i++)
        {
            // Get the current active BlockProcess
            block_process = (BlockProcess)(block_process_list[i]);

            // Update the current active BlockProcess for the view change, and determine whether it's finished.
            if (block_process.UpdateForViewChange() == true)
            {
                // This BlockProcess is finished. Deactivate it.
                Deactivate(block_process);
                i--;
            }
        }
    }

    public static void CancelForBlock(int _x, int _z)
    {
        BlockProcess block_process;

        // Iterate through all active BlockProcess objects...
        for (int i = 0; i < block_process_list.Count; i++)
        {
            // Get the current active BlockProcess
            block_process = (BlockProcess)(block_process_list[i]);

            // If the current active BlockProcess is for the given block, cancel it.
            if ((block_process.x == _x) && (block_process.z == _z))
            {
                Deactivate(block_process);
                i--;
            }
        }
    }

    public void Activate(int _x, int _z, int _process_type, int _nationID, float _delay, float _hit_points_start, float _hit_points_end, float _hit_points_full, float _hit_points_rate, int _battle_flags)
    {
        x = _x;
        z = _z;
        nationID = _nationID;
        hit_points_start = _hit_points_start;
        hit_points_end = _hit_points_end;
        hit_points_full = _hit_points_full;
        hit_points_rate = _hit_points_rate;
        battle_flags = _battle_flags;
        process_type = _process_type;
        hit_points_display = null;
        effect = null;

        state_changed = false;
        sound_played = false;
        hit_points_difference = hit_points_end - hit_points_start;

        effect_position = new Vector3(x * MapView.BLOCK_SIZE + MapView.HALF_BLOCK_SIZE, MapView.instance.GetBlockHeight(x, z) + 5f, -(z * MapView.BLOCK_SIZE + MapView.HALF_BLOCK_SIZE));

        // Determine the times for the various events of this block process.

        float cur_time = Time.time;
        change_state_time = cur_time + _delay;

        BlockData blockData = MapView.instance.GetBlockData(x, z);

        // Record ID of previous nation in this block.
        prevNationID = blockData.nationID;

        // Queue a particle effect if a state transition will take place.
        //Debug.Log("BlockProcess prev block nationID: " + prevNationID + ", process given nationID: " + nationID);
        if ((blockData != null) && (prevNationID != nationID))
        {
            effect_appear_time = change_state_time - EFFECT_HEAD_START_DURATION;
        }
        else
        {
            effect_appear_time = -1;
        }
        
        play_sound_time = effect_appear_time + ((process_type == PROCESS_EVACUATE) ? SOUND_TIME_OFFSET_EVACUATE : SOUND_TIME_OFFSET_OCCUPY);

        //Debug.Log("Block process Activate(): hit_points_start: " + hit_points_start + ", hit_points_end: " + hit_points_end + ", hit_points_full: " + hit_points_full + ", hit_points_rate: " + hit_points_rate);

        // Determine when this process will be finished, and hit points display will be removed.
        if (hit_points_start != hit_points_full) {
            finished_time = change_state_time + (hit_points_difference / (hit_points_rate / 60f)) + DisplayHitPoints.FADE_DURATION;
        } else {
            finished_time = change_state_time;
        }

        finished_time = Math.Max(finished_time, effect_appear_time + EFFECT_DURATION);

        // If the process that is starting is a battle, lock the block (making it insensitive to clicks) until the block changes state (use unscaled time, which is what locks go by).
        if (process_type == PROCESS_BATTLE) {
            MapView.instance.LockBlock(x, z, Time.unscaledTime + _delay);
        }

        //if ((x == 24) && (z == 35)) Debug.Log("BlockProcess.Activate() for block " + x + "," + z + ", new nationID: " + nationID + ", cur_time: " + cur_time + ", change_state_time: " + change_state_time + ", finished_time: " + finished_time + ", state_changed: " + state_changed);

        //Debug.Log("BLOCK PROCESS INIT (at start, block " + x + "," + z + " nationID: " + prevNationID + ", process given nationID: " + nationID);
    }

    public void Deactivate()
    {
        if (hit_points_display != null) {
            DisplayHitPoints.Deactivate(hit_points_display);
        }
    }

    public bool UpdateActivity()
    {
        // Get current time.
        float time = Time.time;

        //if ((x == 20) && (nationID == -1)) Debug.Log("BlockProcess.UpdateActivity() for block " + x + "," + z + ", new nationID: " + nationID + ", time: " + time + ", change_state_time: " + change_state_time + ", finished_time: " + finished_time + ", state_changed: " + state_changed + ", block data: " + MapView.instance.GetBlockData(x, z));

        //Debug.Log("BLOCK PROCESS UpdateActivity() at time " + time + ". Block " + x + "," + z + " nationID: " + MapView.instance.GetBlockData(x, z).nationID);

        if ((time >= change_state_time) && (!state_changed))
        {
            BlockData blockData = MapView.instance.GetBlockData(x, z);

            // Update the block's nationID if necessary.
            if (blockData != null)
            {
                //if ((x == 24) && (z == 35)) Debug.Log("BlockProcess.UpdateActivity() for block " + x + "," + z + ", new nationID: " + nationID + ", cur_time: " + time + ", change_state_time: " + change_state_time + ", finished_time: " + finished_time + ", state_changed: " + state_changed);

                // Let the MapView know that this block has been captured.
                MapView.instance.UpdateBlock(x, z, nationID, (battle_flags & (int)GameData.BattleFlags.FAST_CRUMBLE) != 0);
            }
            
            state_changed = true;

            if ((finished_time > change_state_time) && (hit_points_start != hit_points_full))
            {
                // Spawn a DisplayHitPoints object to display the growth in hit points for the remainder of this process' time.
                float duration = (hit_points_end - hit_points_start) / (hit_points_rate / 60f);
                hit_points_display = DisplayHitPoints.ActivateNew(x, z, hit_points_start, hit_points_end, hit_points_full, duration, DisplayHitPoints.TransitionType.LINEAR, true, false);
            }

            //Debug.Log("BLOCK PROCESS STATE CHANGE");
        }

        if ((prevNationID != nationID) && (time >= play_sound_time) && (!sound_played))
        {
            // Play sound. 
            if (process_type == PROCESS_OCCUPY) {
                Sound.instance.PlayInWorld(Sound.instance.occupy_land, effect_position);
            } else if (process_type == PROCESS_EVACUATE) {
                Sound.instance.PlayInWorld(Sound.instance.evac_land, effect_position);
            } else { // PROCESS_BATTLE
                if ((prevNationID == GameData.instance.nationID) && (nationID != GameData.instance.nationID)) {
                    // Play pitched-down evacuate sound if the player's nation has lost a battle.
                    Sound.instance.PlayInWorld(Sound.instance.evac_land, effect_position, Sound.DEFAULT_AUDIO_VOLUME, 1, 0.8f);
                } else {
                    Sound.instance.PlayInWorld(Sound.instance.occupy_land, effect_position);
                }                
            }

            sound_played = true;
        }

        if ((effect_appear_time != -1) && (time >= effect_appear_time) && (effect == null))
        {
            // Position and play the particle effect.
            effect = MemManager.instance.GetTakeLandParticleObject();
            effect.transform.position = effect_position;
            effect.transform.GetChild(0).GetComponent<ParticleSystem>().Play();
            //effect.transform.GetChild(0).GetChild(0).GetComponent<ParticleSystem>().startColor = (process_type == PROCESS_EVACUATE) ? evacuate_effect_color : GameData.instance.nationColor;
        }

        if (time >= finished_time)
        {
            //Debug.Log("BLOCK PROCESS FINISH");

            if (effect != null)
            {
                // Stop and release the particle effect.
                effect.transform.GetChild(0).GetComponent<ParticleSystem>().Stop();
                MemManager.instance.ReleaseTakeLandParticleObject(effect);
                effect = null;
            }

            return true;
        }

        return false;
    }

    public bool UpdateForViewChange()
    {
        if (MapView.instance.BlockOutsideViewData(x,z)) {
            return true;
        }

        return false;
    }
}
