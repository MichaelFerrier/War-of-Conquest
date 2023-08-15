using UnityEngine;
using UnityEngine.UI;
using System.Collections;

public class LandscapeObject : MonoBehaviour
{
    int blockX, blockZ;
    int objectID = -1;
    BlockData block_data = null;
    ObjectData object_data = null;

    bool objects_instantiated = false;
    public GameObject object0 = null;

    public void Initialize(int _blockX, int _blockZ)
    {
        // Cache data about this block and object.
        blockX = _blockX;
        blockZ = _blockZ;
        block_data = MapView.instance.GetBlockData(_blockX, _blockZ);
        objectID = block_data.objectID;
        object_data = ObjectData.GetObjectData(objectID);

        // Instantiate the 3D objects associated with this build.
        InstantiateObjects();
    }

    public void CleanUp()
    {
        // Delete the 3D objects associated with this build.
        DeleteObjects();

        // Destroy this GameObject.
        Destroy(gameObject);
    }

    private void InstantiateObjects()
    {
		if (object_data == null) {
			Debug.Log("ERROR: LandscapeObject.InstantiateObjects(): object_data is null!");
			return;
		}

        // Instantiate the landscape object from its prefab, and set its position.
        object0 = Object.Instantiate(object_data.GetPrefab()) as GameObject;
        object0.transform.position = MapView.instance.GetBlockCenterWorldPos(blockX, blockZ);
        if (object0 != null) object_data.PrepObject(object0);

        if (object_data.original_name.Equals("Fertile Soil")) 
        {
            // Apply random rotation around center, for variation. Use random hash based on block position, so it is replicable.
            object0.transform.localEulerAngles = object0.transform.localEulerAngles + new Vector3(0, (GameData.instance.xxhash.GetHashFloat(blockX, blockZ) < 0.5) ? 0 : 90, 0);
        }
        if (object_data.original_name.Equals("Fresh Water")) 
        {
            // Apply random rotation around center, for variation. Use random hash based on block position, so it is replicable.
            object0.transform.localEulerAngles = object0.transform.localEulerAngles + new Vector3(0, GameData.instance.xxhash.GetHashFloat(blockX, blockZ) * 200 + 120, 0);
        }
        else if (object_data.original_name.Equals("Cryptid Colony")) 
        {
            // Apply random rotation around center, for variation. Use random hash based on block position, so it is replicable.
            object0.transform.localEulerAngles = object0.transform.localEulerAngles + new Vector3(0, GameData.instance.xxhash.GetHashFloat(blockX, blockZ) * 80 + 170, 0);
        }
        else if (object_data.original_name.Equals("Quartz Deposit")) 
        {
            // Apply random rotation around center, for variation. Use random hash based on block position, so it is replicable.
            object0.transform.localEulerAngles = object0.transform.localEulerAngles + new Vector3(0, GameData.instance.xxhash.GetHashFloat(blockX, blockZ) * 100 + 260, 0);
        }

        // TESTING
        if ((object0 != null) && (object0.transform.childCount > 0) && (object0.transform.GetChild(0).childCount > 0) && (object0.transform.GetChild(0).GetChild(0).GetComponent<TextMesh>() != null)) {
            object0.transform.GetChild(0).GetChild(0).GetComponent<TextMesh>().text = object_data.name;
        }
        
        // Record that the objects have been instantiated.
        objects_instantiated = true;
    }

    private void DeleteObjects()
    {
        if (object0)
        {
            ObjectData.DestroyAllMaterials(object0);
            Object.Destroy(object0);
            object0 = null;
        }
    }

    public void SetRandomTargetBlock(int _blockX, int _blockZ, BlockData _block_data)
    {
        _block_data.target_x = _blockX + Random.Range(-5, 5);
        _block_data.target_z = _blockZ + Random.Range(-5, 5);
    }
}
