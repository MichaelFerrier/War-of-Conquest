using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class StorageMeter : MonoBehaviour
{
    public GameObject empty, fill, energyCylinder, manpowerCylinder;
    public int blockX, blockZ;
    public float level, startLevel, endLevel;

    public static ArrayList storage_meter_list = new ArrayList();

    public static GameObject Get(BuildData _buildData, int _blockX, int _blockZ)
    {
        // Determine height at which to place the storage meter.
        float height;
        switch (_buildData.ID)
        {
            case 18: height = 11.5f; break; // Fuel Tank I 
            case 19: height = 14f; break; // Fuel Tank II
            case 20: height = 18.5f; break; // Fuel Tank III
            case 66: height = 8.5f; break; // Agro Colony I 
            case 67: height = 9.5f; break; // Agro Colony II 
            case 68: height = 10f; break; // Agro Colony III
            case 69: height = 12f; break; // Hydropnic Garden I 
            case 70: height = 16f; break; // Hydropnic Garden II 
            case 71: height = 19.5f; break; // Hydropnic Garden III 
            case 99: height = 9.5f; break; // Energy Vortex I 
            case 100: height = 11f; break; // Energy Vortex II
            case 101: height = 12.5f; break; // Energy Vortex III
            case 102: height = 9.5f; break; // Earth Chakra I
            case 103: height = 9.5f; break; // Earth Chakra II
            case 104: height = 9.5f; break; // Earth Chakra III
            case 200: height = 10f; break; // Red Shard
            case 201: height = 10f; break; // Green Shard
            case 202: height = 10f; break; // Blue Shard
            default: height = 0f; break;
        }

         // Set up a new StorageMeter object.
        GameObject storageMeterObject = MemManager.instance.GetStorageMeterObject();
        storageMeterObject.transform.position = MapView.instance.GetBlockCenterWorldPos(_blockX, _blockZ) + new Vector3(0, height, 0);
        storageMeterObject.transform.localEulerAngles = new Vector3(0f, 45f, 0f);
        storageMeterObject.SetActive(true);

        StorageMeter storageMeter = storageMeterObject.GetComponent<StorageMeter>(); 
        storageMeter.energyCylinder.SetActive((_buildData.type == BuildData.Type.ENERGY_STORAGE) || (_buildData.type == BuildData.Type.SHARD));
        storageMeter.manpowerCylinder.SetActive(_buildData.type == BuildData.Type.MANPOWER_STORAGE);

        // Record location
        storageMeter.blockX = _blockX;
        storageMeter.blockZ = _blockZ;

        // Add the new storage meter to the list of active storage meters.
        storage_meter_list.Add(storageMeter);

        // Perform initial update of the meter
        storageMeter.UpdateMeter(true);

        return storageMeterObject;
    }

    public static void Release(GameObject _storageMeter)
    {
        // Remove the given storage meter from the list of active storage meters.
        storage_meter_list.Remove(_storageMeter);

        _storageMeter.SetActive(false);
        MemManager.instance.ReleaseStorageMeterObject(_storageMeter);
    }

    public static void UpdateNationMeters(int _nationID)
    {
        foreach (StorageMeter storageMeter in storage_meter_list)
        {
            BlockData block_data = MapView.instance.GetBlockData(storageMeter.blockX, storageMeter.blockZ);

            if ((block_data != null) && (block_data.owner_nationID == _nationID) && (block_data.build_object != null) && (block_data.build_object.storageMeter == storageMeter.gameObject)) {
                storageMeter.UpdateMeter(block_data);
            }
        }
    }

    public void UpdateMeter(bool _instant = false)
    {
        BlockData block_data = MapView.instance.GetBlockData(blockX, blockZ);

        if (block_data != null) {
            UpdateMeter(block_data, _instant);
        }
    }

    public void UpdateMeter(BlockData _block_data, bool _instant = false)
    {
        float new_level = 0f;

        if (_block_data.owner_nationID == _block_data.nationID)
        {
            NationData nationData = GameData.instance.GetNationData(_block_data.owner_nationID);

            if (nationData != null) 
            {
                if (_block_data.build_object.build_data.type == BuildData.Type.MANPOWER_STORAGE)
                {
                    new_level = nationData.sharedManpowerFill;
                }
                else if (_block_data.build_object.build_data.type == BuildData.Type.ENERGY_STORAGE)
                {
                    new_level = nationData.sharedEnergyFill;
                } 
                else if (_block_data.build_object.build_data.type == BuildData.Type.SHARD)
                {
                    switch (_block_data.objectID)
                    {
                        case 200: new_level = GameData.instance.shardRedFill; break;
                        case 201: new_level = GameData.instance.shardGreenFill; break;
                        case 202: new_level = GameData.instance.shardBlueFill; break;
                    }
                }
            }
        }
        
        if (_instant)
        {
            SetLevel(new_level);
        }
        else
        {
            StartCoroutine(AnimateLevel(new_level));
        }
    }

    public IEnumerator AnimateLevel(float _new_level)
    {
        startLevel = level;
        endLevel = _new_level;

        // Wait bfore starting animation.
        yield return new WaitForSeconds(2f);

        float startTime = Time.unscaledTime;
        float duration = 2f;

        while (Time.unscaledTime < (startTime + duration))
        {
            // Set the level to current interpolated value and wait until next frame.
            SetLevel(Mathf.SmoothStep(startLevel, endLevel, ((Time.unscaledTime - startTime) / duration)));
            yield return null;
        }

        // Set level to final value.
        SetLevel(endLevel);
    }

    public void SetLevel(float _level)
    {
        // Record the new level
        level = _level;

        // Display the determined fill level.
        fill.transform.localScale = new Vector3(1f, level, 1f);
        empty.transform.localScale = new Vector3(1f, 1f - level, 1f);
        fill.SetActive(level > 0f);
        empty.SetActive(level < 1f);
    }
}